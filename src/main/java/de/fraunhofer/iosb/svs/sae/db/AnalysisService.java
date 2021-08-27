package de.fraunhofer.iosb.svs.sae.db;

import de.fraunhofer.iosb.svs.sae.FTPClientService;
import de.fraunhofer.iosb.svs.sae.dto.AnalysisDTO;
import de.fraunhofer.iosb.svs.sae.dto.PolicyAnalysisDTO;
import de.fraunhofer.iosb.svs.sae.exceptions.*;
import de.fraunhofer.iosb.svs.sae.workflowmanager.datasource.CachedOntModelHandler;
import de.fraunhofer.iosb.svs.sae.workflowmanager.exceptions.InvalidOntologyException;
import de.fraunhofer.iosb.svs.sae.workflowmanager.model.Thing;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.DataProperty.*;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.ObjectProperty.CONTAINS_URI;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.OntologyDependency.ONTOLOGY_DEPENDENCY_URI;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Policy.POLICY_BASED_ANALYSIS_URI;
import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Policy.POLICY_BASED_ANALYSIS_URI_VERSION;

@Service
public class AnalysisService {
    public static final String ANALYSIS = "Analysis";
    public static final String APP = "App";
    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private final AnalysisRepository analysisRepository;
    private final AppRepository appRepository;
    private final WebClient webclient;
    private final UrlValidator urlValidator;
    private final FTPClientService ftpClientService;
    private final CachedOntModelHandler cachedOntModelHandler;
    private final Resource policyBasedAnalysisResource;

    @Autowired
    public AnalysisService(AnalysisRepository analysisRepository, AppRepository appRepository,
            FTPClientService ftpClientService, CachedOntModelHandler cachedOntModelHandler,
            @Value("classpath:ontologies/policy-based-analysis.owl") Resource policyBasedAnalysisResource) {
        this.analysisRepository = analysisRepository;
        this.appRepository = appRepository;
        this.ftpClientService = ftpClientService;
        this.cachedOntModelHandler = cachedOntModelHandler;
        this.policyBasedAnalysisResource = policyBasedAnalysisResource;
        this.webclient = WebClient.create();
        String[] schemes = { "http", "https" };
        // TODO allow local urls only for dev?
        urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
    }

    public Analysis addAnalysis(String key, AnalysisDTO analysisDTO, String analysisHash)
            throws ResourceAlreadyExistsException {
        App app = appRepository.findById(key).orElseThrow(() -> new ResourceNotFoundException(APP, key));

        Analysis analysis;

        if (analysisRepository.existsByHash(analysisHash)) {
            throw new ResourceAlreadyExistsException(ANALYSIS, "hash", analysisHash);
        } else {
            Set<PolicyAnalysis> policyAnalyses = fetchAndAddPolicyAnalyses(analysisDTO.getPolicyAnalyses());

            analysis = analysisRepository.save(new Analysis(analysisDTO.getName(), analysisDTO.getDescription(),
                    app.getKey(), analysisDTO.getTargetSystemId(), policyAnalyses, analysisHash));

        }

        return analysis;
    }

    /**
     * Fetches every policy analysis and adds it.
     *
     * @param policyAnalysesLinks
     * @return
     */
    private Set<PolicyAnalysis> fetchAndAddPolicyAnalyses(List<String> policyAnalysesLinks) {
        Set<PolicyAnalysis> policyAnalyses = new HashSet<>();
        for (String policyAnalysisLink : policyAnalysesLinks) {
            validateUrl(policyAnalysisLink);
            // pull policy analysis, pull model
            // TODO check if policy already exists? how to update? according to last changed
            // value?
            PolicyAnalysisDTO policyAnalysisDTO = fetchPolicyAnalysisDTO(policyAnalysisLink);
            PolicyAnalysis policyAnalysis = new PolicyAnalysis(policyAnalysisDTO.getLocalName(),
                    policyAnalysisDTO.getDescription(), policyAnalysisDTO.getModelLink());
            // fetch model
            DataBuffer ontologyBuffer = fetchModelData(policyAnalysisDTO);

            // reading policy-based-analysis ontology and adding it to the document manager
            // so that it can be used as "imported" ontology
            // TODO only add if it is not already present in the document manager
            OntModel policyBasedAnalysis = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            try {
                policyBasedAnalysis.read(policyBasedAnalysisResource.getInputStream(), "RDF/XML");
            } catch (IOException e) {
                log.error("Local file {} not found", policyBasedAnalysisResource.getFilename());
                throw new RuntimeException(
                        "Error reading local file '" + policyBasedAnalysisResource.getFilename() + "'");
            }
            OntDocumentManager.getInstance().addModel(POLICY_BASED_ANALYSIS_URI_VERSION, policyBasedAnalysis);

            // add model to fuseki. pull ontologydependency files. get query and create
            // plugin
            if (ontologyBuffer != null) {
                OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM); // TODO Reasoning?
                ontModel.read(ontologyBuffer.asInputStream(), "RDF/XML");
                ontModel.listImportedOntologyURIs().forEach(log::debug);

                // TODO maybe this in the wrong place here. These should probably be only
                // downloaded when the workflows are already created.
                // TODO Because here all ontodeps will be downloaded but maybe we don't need all
                // of them but only those who are not already in the model
                // TODO also the links should probably not point directly to the files but to a
                // meta object that has a last changed values so we do not need to redownload
                // all ontodeps
                fetchAndAdaptOntologyDependencies(ontModel);

                // TODO plugin handler
                AnalysisPlugin p = new AnalysisPlugin();
                p.setPluginClasspath("de.fraunhofer.iosb.svs.sparql_security_analysis_plugin.Plugin");
                p.setPluginFileName("sparql_security_analysis_plugin-0.0.2-SNAPSHOT.jar");
                p.setResultClasspath("de.fraunhofer.iosb.svs.sparql_security_analysis_plugin.Report");
                policyAnalysis.setAnalysisPlugin(p); // which plugin to use - pulled from hub?

                // TODO is this the best way to handle the query?
                Individual policyImplementation = ontModel.getIndividual(policyAnalysisDTO.getUri());
                Individual queryIndividual = Thing.getMustExistIndividual(policyImplementation, CONTAINS_URI);
                Literal query = Thing.getMustExistLiteral(queryIndividual, QUERY_URI);
                policyAnalysis.setQuery(URLEncoder.encode(query.getString(), StandardCharsets.UTF_8));

                // OntModel should have two ontologies: policy based analysis (because it is
                // imported) and the ontology for the policy
                // extracting the ontology for the policy
                Ontology ontology = Iterables.getOnlyElement(ontModel.listOntologies()
                        .filterDrop(tempOntology -> POLICY_BASED_ANALYSIS_URI.equals(tempOntology.getURI())).toList());
                String graphName = ontology.getURI();
                // uploading to fuseki (only base model, so the imported policy based analysis
                // isnt added)
                cachedOntModelHandler.addModel(graphName, ontModel.getBaseModel());
                policyAnalysis.setGraphName(graphName);
                policyAnalyses.add(policyAnalysis);
            } else {
                throw new RuntimeException("No model from model link");
            }
        }
        return policyAnalyses;
    }

    /**
     * Gets all ontology dependencies from the model. For every ontology dependency:
     * Extracts the downloadLink property. Downloads the file. Uploads the file to
     * ftp server. Replaces the downloadLink property
     *
     * @param ontModel ontModel of the policy analysis. Is being modified.
     */
    private void fetchAndAdaptOntologyDependencies(OntModel ontModel) {
        List<Individual> ontologyDependencies = ontModel.getOntClass(ONTOLOGY_DEPENDENCY_URI).listInstances()
                .mapWith(OntResource::asIndividual).toList();
        Map<String, DataBuffer> filesToSave = new HashMap<>();
        for (Individual ontologyDependency : ontologyDependencies) {
            String downloadLink = getDownloadLink(ontologyDependency);
            validateUrl(downloadLink);
            String prefix = getPrefix(ontologyDependency);

            // fetch from link
            log.debug("Fetch ontology dependency with prefix '{}' from '{}'", prefix, downloadLink);
            DataBuffer ontologyDepBuffer = webclient.get().uri(downloadLink).accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve().bodyToMono(DataBuffer.class).block();
            // TODO check if it is valid ontology file?
            // TODO what to take as filename?
            //
            filesToSave.put(ftpClientService.getFilenameForOwl(prefix), ontologyDepBuffer);
        }
        // upload to ftp
        log.debug("Going to upload to ftp");
        ftpClientService.uploadFiles(filesToSave);
        // replace download link with new uri
        for (Individual ontologyDependency : ontologyDependencies) {
            String filename = ftpClientService.getFilenameForOwl(getPrefix(ontologyDependency));
            String newDownloadLink = ftpClientService.getUriFor(filename);
            DatatypeProperty dataProperty = ontologyDependency.getOntModel().getDatatypeProperty(DOWNLOAD_LINK_URI);
            if (dataProperty == null) {
                throw new InvalidOntologyException("Data Property '" + DOWNLOAD_LINK_URI + "' not found");
            }
            ontologyDependency.setPropertyValue(dataProperty, null);
            ontologyDependency.addProperty(dataProperty, newDownloadLink);
            log.debug("Replaced downloadLink for ontology dependency '{}' with '{}'", ontologyDependency.getLocalName(),
                    newDownloadLink);
        }
    }

    /**
     * Fetches the model data.
     *
     * @param policyAnalysisDTO
     * @return
     */
    private DataBuffer fetchModelData(PolicyAnalysisDTO policyAnalysisDTO) {
        String modelLink = policyAnalysisDTO.getModelLink();
        validateUrl(modelLink);
        log.debug("Fetch policy analysis model from link '{}'", modelLink);
        return webclient.get().uri(modelLink).accept(MediaType.APPLICATION_OCTET_STREAM).retrieve()
                .bodyToMono(DataBuffer.class).block();
    }

    /**
     * Fetches the policy analysis DTO meaning only the meta information about a
     * policy analysis is retrieved.
     *
     * @param policyAnalysisLink
     * @return
     */
    private PolicyAnalysisDTO fetchPolicyAnalysisDTO(String policyAnalysisLink) {
        try {
            log.info("Fetch Policy Analysis from '{}'", policyAnalysisLink);
            PolicyAnalysisDTO policyAnalysisDTO = webclient.get().uri(policyAnalysisLink)
                    .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(PolicyAnalysisDTO.class)
                    .onErrorResume(e -> Mono.error(new AnalysisCreationException(
                            "Could not fetch policy analysis from hub: " + policyAnalysisLink, e)))
                    .block();
            if (policyAnalysisDTO == null) {
                throw new AnalysisHubException("Could not get Policy Analysis '" + policyAnalysisLink + "'");
            }
            return policyAnalysisDTO;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                // policy analysis does not exist
                log.warn("Analysis Hub has no policy analysis at '{}", policyAnalysisLink);
                throw new AnalysisHubException("Analysis Hub has no policy analysis at '" + policyAnalysisLink + "'");
            } else {
                // pass other errors
                log.error("this error", ex);
                throw ex;
            }
        }
    }

    private String getPrefix(Individual ontologyDependency) {
        return Thing.getMustExistLiteral(ontologyDependency, PREFIX_URI).getString();
    }

    private String getDownloadLink(Individual ontologyDependency) {
        return Thing.getMustExistLiteral(ontologyDependency, DOWNLOAD_LINK_URI).getString();
    }

    private void validateUrl(String url) {
        if (!urlValidator.isValid(url)) {
            log.debug("'{}' not a valid url", url);
            throw new InvalidUrlException("'" + url + "' is not a valid url");
        }
    }

    public Analysis update(AnalysisDTO analysisDTO) {
        Analysis updatedAnalysis = findByHash(analysisDTO.getHash());

        if (updatedAnalysis != null) {
            Set<PolicyAnalysis> policyAnalyses = fetchAndAddPolicyAnalyses(analysisDTO.getPolicyAnalyses());

            updatedAnalysis.setDescription(analysisDTO.getDescription());
            updatedAnalysis.setName(analysisDTO.getName());
            updatedAnalysis.setTargetSystemId(analysisDTO.getTargetSystemId());

            // TODO: Fix
            // The setPolicyAnalysis method fails for some reason (descritpion is set to
            // null)
            // updatedAnalysis.setPolicyAnalyses(policyAnalyses);

            analysisRepository.save(updatedAnalysis);
        }

        return updatedAnalysis;
    }

    public List<Analysis> findAll(String key) {
        // TODO key?
        return analysisRepository.findAll();
    }

    public Analysis findById(Long id) {
        return analysisRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Analysis", id));
    }

    public Analysis findByHash(String analysisHash) {
        Analysis exampleAnalysis = new Analysis();
        exampleAnalysis.setHash(analysisHash);
        Example<Analysis> example = Example.of(exampleAnalysis);

        Optional<Analysis> analysisQuery = analysisRepository.findOne(example);
        Analysis analysis = null;
        if (analysisQuery.isPresent()) {
            analysis = analysisQuery.get();
        } else {
            throw new ResourceNotFoundException("No Analysis found with hash:" + analysisHash);
        }
        return analysis;
    }

    public void deleteById(Long id) {
        // nalysisRepository.deleteById(id);
        throw new UnsupportedOperationException();
    }

    public void deleteByHash(String analysisHash) {
        Analysis analysis = findByHash(analysisHash);
        if (analysis != null) {
            analysisRepository.deleteById(analysis.getId());
        } else {
            throw new ResourceNotFoundException("No Analysis found with hash:" + analysisHash);
        }
    }
    
    public String createHash(List<String> policiesList, Long tragetSystemId) {
        StringJoiner sj = new StringJoiner(":", "[", "]");
        
        for (String policyUrl : policiesList) {
            sj.add(policyUrl);
        }
        
        sj.add(tragetSystemId.toString());
        
        return  Hashing.sha256()
                .hashString(sj.toString(), StandardCharsets.UTF_8)
                .toString();
    }
}
