package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.db.Analysis;
import de.fraunhofer.iosb.svs.sae.db.PolicyAnalysis;
import de.fraunhofer.iosb.svs.sae.db.PolicyAnalysisRepository;
import de.fraunhofer.iosb.svs.sae.db.WorkflowRepository;
import de.fraunhofer.iosb.svs.sae.restclient.sme.TargetSystem;
import de.fraunhofer.iosb.svs.sae.workflowmanager.WorkflowManager;
import de.fraunhofer.iosb.svs.sae.workflowmanager.datasource.CachedOntModelHandler;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.fraunhofer.iosb.svs.sae.workflowmanager.model.Policy.POLICY_BASED_ANALYSIS_URI_VERSION;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class WorkflowManagerTest {

    @Mock
    private CachedOntModelHandler cachedOntModelHandler;
    @Mock
    private PolicyAnalysisRepository policyAnalysisRepository;
    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private FTPClientService ftpClientService;

    @InjectMocks
    private WorkflowManager workflowManager;

    @Before
    public void before() {
        assertThat(cachedOntModelHandler).isNotNull();
        assertThat(policyAnalysisRepository).isNotNull();
        assertThat(workflowRepository).isNotNull();
        assertThat(ftpClientService).isNotNull();
        assertThat(workflowManager).isNotNull();

        OntModel policyBasedAnalysis = loadModel("ontologies/policy-based-analysis.owl");
        OntDocumentManager.getInstance().addModel(POLICY_BASED_ANALYSIS_URI_VERSION, policyBasedAnalysis);
        when(cachedOntModelHandler.getOntModel(POLICY_BASED_ANALYSIS_URI_VERSION)).thenReturn(policyBasedAnalysis);
    }

    @Test
    public void sequentialTest() {
        // set up
        Analysis analysis = setUpModel("ontologies/sequential-test.owl");
        workflowManager.createWorkflows(analysis);
        // validate
    }

    @Test
    public void parallelTest() {
        // set up
        Analysis analysis = setUpModel("ontologies/parallel-test.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void parallelTest2() {
        // set up
        Analysis analysis = setUpModel("ontologies/parallel-test-2.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void parallelTest3() {
        // set up
        Analysis analysis = setUpModel("ontologies/parallel-test-3.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void parallelTest4() {
        // set up
        Analysis analysis = setUpModel("ontologies/parallel-test-4.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void mergeTest1() {
        // set up
        Analysis analysis = setUpModel("ontologies/merge-test-1.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void mergeTest2() {
        // set up
        Analysis analysis = setUpModel("ontologies/merge-test-2.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void mergeTest3() {
        // set up
        Analysis analysis = setUpModel("ontologies/merge-test-3.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void mergeTest4() {
        // set up
        Analysis analysis = setUpModel("ontologies/merge-test-4.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void doubleMergeTest() {
        // set up
        Analysis analysis = setUpModel("ontologies/double-merge-test.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cyclicTest() {
        // set up
        Analysis analysis = setUpModel("ontologies/cyclic-test.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void complexTest() {
        // set up
        Analysis analysis = setUpModel("ontologies/complex-test-1.owl");
        workflowManager.createWorkflows(analysis);
    }

    @Test
    public void complexTest2() {
        // set up
        Analysis analysis = setUpModel("ontologies/complex-test-2.owl");
        workflowManager.createWorkflows(analysis);
    }

    private Analysis setUpModel(String modelName){

        OntModel exampleOntology = loadModel(modelName, OntModelSpec.OWL_DL_MEM_RULE_INF);
        String graphName = "https://example.com/test/sequential-test_Ontology";

        when(cachedOntModelHandler.getOntModel(graphName)).thenReturn(exampleOntology);

        Analysis analysis = Mockito.mock(Analysis.class);
        TargetSystem targetSystem = Mockito.mock(TargetSystem.class);
        when(targetSystem.getOntologyPath()).thenReturn("ftp://ftp/uploads/sysont.owl");
        when(targetSystem.getTaskIds()).thenReturn(Collections.emptySet());
        when(targetSystem.getOntologyDependencyIds()).thenReturn(Collections.emptySet());
        when(analysis.getTargetSystem()).thenReturn(targetSystem);
        PolicyAnalysis policyAnalysis = new PolicyAnalysis("test", "test", "http://test/test");
        policyAnalysis.setGraphName(graphName);
        policyAnalysis.setAnalysis(analysis);

        when(analysis.getPolicyAnalyses()).thenReturn(Collections.singleton(policyAnalysis));

        return analysis;
    }

    private OntModel loadModel(String filename, OntModelSpec ontModelSpec) {
        //ClassLoader classLoader = getClass().getClassLoader();
        //File file = new File(classLoader.getResource("ontologies/policy-based-analysis.owl").getFile());
        OntModel model = ModelFactory.createOntologyModel(ontModelSpec);
        InputStream in = RDFDataMgr.open(filename);
        model.read(in, "RDF/XML");
        return model;
    }

    private OntModel loadModel(String filename) {
        return loadModel(filename, OntModelSpec.OWL_DL_MEM);
    }
}
