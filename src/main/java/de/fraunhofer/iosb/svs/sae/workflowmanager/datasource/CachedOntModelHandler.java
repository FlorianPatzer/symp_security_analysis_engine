package de.fraunhofer.iosb.svs.sae.workflowmanager.datasource;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class CachedOntModelHandler implements OntModelGetter, OntModelAdder {

    private static final Logger log = LoggerFactory.getLogger(CachedOntModelHandler.class);

    private String fusekiDatasetEndpoint;
    private final FusekiModelMaker modelMaker;
    private final Map<String, OntModel> ontModels = new HashMap<>();

    public CachedOntModelHandler(@Value("${fuseki.dataset.endpoint}") String fusekiDatasetEndpoint) {
        this.fusekiDatasetEndpoint = fusekiDatasetEndpoint;
        log.info("Using Fuseki at {}", fusekiDatasetEndpoint);
        this.modelMaker = new FusekiModelMaker(fusekiDatasetEndpoint);
    }

    @Override
    public OntModel getOntModel(String name) {
        if (ontModels.containsKey(name)) {
            log.debug("Found Model in cache: {}", name);
            return ontModels.get(name);
        } else {
            return getAndAddModel(name);
        }
    }

    private OntModel getAndAddModel(String name) {
        Model model = modelMaker.getModel(name);
        //OntModelSpec.OWL_DL_MEM
        //OWL_DL_MEM_TRANS_INF
        log.debug("Creating OntologyModel: {}", name);
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, modelMaker, model);
        ontModels.put(name, ontModel);
        return ontModel;
    }

    private void removeModelFromCache(String name) {
        if (ontModels.containsKey(name)) {
            log.debug("Remove Model from cache: {}", name);
            ontModels.remove(name);
        } else {
            log.debug("Cannot remove from cache: '{}': not in it", name);
        }
    }

    @Override
    public void addModel(String name, Model ontModel) {
        removeModelFromCache(name);
        modelMaker.createModel(name, ontModel);
        // add to cache?
    }

    // TODO reload model
}
