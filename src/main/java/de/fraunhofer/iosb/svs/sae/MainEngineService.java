package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.db.*;
import de.fraunhofer.iosb.svs.sae.dto.ReportNotification;
import de.fraunhofer.iosb.svs.sae.exceptions.*;
import de.fraunhofer.iosb.svs.sae.plugin.AnalysisPluginAbstract;
import de.fraunhofer.iosb.svs.sae.plugin.AnalysisReportAbstract;
import de.fraunhofer.iosb.svs.sae.restclient.camunda.CamundaClient;
import de.fraunhofer.iosb.svs.sae.restclient.camunda.CamundaProcessInstance;
import de.fraunhofer.iosb.svs.sae.restclient.camunda.CamundaProcessInstanceHistory;
import de.fraunhofer.iosb.svs.sae.restclient.sme.SmeClientService;
import de.fraunhofer.iosb.svs.sae.restclient.sme.TargetSystem;
import de.fraunhofer.iosb.svs.sae.security.RestTemplateWithReloadableTruststore;
import de.fraunhofer.iosb.svs.sae.workflowmanager.WorkflowManager;
import de.fraunhofer.iosb.svs.sae.workflowmanager.exceptions.InvalidOntologyException;

import org.apache.commons.collections.map.HashedMap;
import org.openqa.selenium.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

@Service
public class MainEngineService {
    private static final Logger log = LoggerFactory.getLogger(MainEngineService.class);

    private final AsyncService asyncService;
    private final SmeClientService smeClientService;
    private final WorkflowManager workflowManager;
    private final CamundaClient camundaClient;
    private final WorkerService workerService;
    private final FTPClientService ftpClientService;

    private final AppRepository appRepository;
    private final AnalysisRepository analysisRepository;
    private final PolicyAnalysisRepository policyAnalysisRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final PolicyAnalysisReportRepository policyAnalysisReportRepository;

    @Autowired
    private RestTemplateWithReloadableTruststore restTemplateCreator;

    private Map<Long, AnalysisState> runningAnalyses;
    // maps analysis
    private Map<String, Map<String, Object>> loadedPlugins;

    //
    private Map<Long, List<String>> startedAnalyses;

    @Autowired
    public MainEngineService(AsyncService asyncService, SmeClientService smeClientService,
            WorkflowManager workflowManager, CamundaClient camundaClient, DockerService dockerService,
            KubeService kubeService, FTPClientService ftpClientService, AppRepository appRepository,
            AnalysisRepository analysisRepository, PolicyAnalysisRepository policyAnalysisRepository,
            AnalysisReportRepository analysisReportRepository,
            PolicyAnalysisReportRepository policyAnalysisReportRepository,
            @Value("${spring.profiles.active}") String activeProfile) {
        this.asyncService = asyncService;
        this.smeClientService = smeClientService;
        this.workflowManager = workflowManager;
        this.camundaClient = camundaClient;
        this.ftpClientService = ftpClientService;
        this.appRepository = appRepository;
        this.analysisRepository = analysisRepository;
        this.policyAnalysisRepository = policyAnalysisRepository;
        this.analysisReportRepository = analysisReportRepository;
        this.policyAnalysisReportRepository = policyAnalysisReportRepository;

        if (activeProfile.equals("k8s") || activeProfile.equals("gitlab")) {
            this.workerService = kubeService;
            log.info("Using Kubernetes WorkerService");
        } else {
            this.workerService = dockerService;
            log.info("Using Docker WorkerService");
        }

        runningAnalyses = new HashMap<>();
        loadedPlugins = new HashMap<>();
        startedAnalyses = new HashMap<Long, List<String>>();
    }

    public Map<String, String> checkAnalysisStatus(Analysis analysis) {
        Map<String, String> output = new HashMap<String, String>();

        if (startedAnalyses.containsKey(analysis.getId())) {
            output.put("status", "Running");
        } else {
            output.put("status", "Idle");
        }

        return output;
    }

    public Map<String, String> startAnalysis(Analysis analysis, String appKey) throws InterruptedException {
        // App app = getApp(appKey);
        Map<String, String> output = new HashMap<String, String>();
        Long analysisId = analysis.getId();

        List<String> appSubscribers;

        if (!startedAnalyses.containsKey(analysisId)) {
            appSubscribers = new ArrayList<String>();
            appSubscribers.add(appKey);
            startedAnalyses.put(analysisId, appSubscribers);

            // Update start time and running status
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            analysis.setLastInvocation(timestamp);

            // Generate a new analysis report with the timestamp and add it to the analysis
            // adding as general analysis report, but also as current analysis report
            AnalysisReport analysisReport = new AnalysisReport(timestamp);
            analysis.addAnalysisReport(analysisReport);
            analysis.setCurrentAnalysisReport(analysisReport);
            analysisReportRepository.save(analysisReport);
            analysisRepository.save(analysis);

            fillAnalysisWithTargetSystem(analysis);
            log.info("TargetSystem for Analysis {} with id {} found", analysis.getName(), analysisId);
            log.debug(analysis.getTargetSystem().toString());
            // test Satisfiability
            checkSatisfiability(analysis);
            log.info("Analysis {} with id {} is satisfiable", analysis.getName(), analysisId);

            log.info("Analysis {} with id {} started", analysis.getName(), analysis.getId());

            Set<String> workerImages = workflowManager.createWorkflows(analysis);

            for (String workerImage : workerImages) {
                String workerName = "";

                // Cut the worker image name if an external image is used
                // The worker image name could happen to be longer than 63 chars and this causes
                // error when deploying
                if (workerImage.contains("/")) {
                    workerName = workerImage.substring(workerImage.lastIndexOf("/") + 1);
                } else {
                    workerName = workerImage;
                }

                // Cut await also the colon and any version slug afterwards
                if (workerName.contains(":")) {
                    workerName = workerName.substring(0, workerName.lastIndexOf(":"));
                }

                log.info("Creating a container with name: {} and image: {}", workerName, workerImage);
                workerService.hasOrStartWorker(workerImage, workerName);
            }

            // deploy processes
            createCamundaDeployment(analysis);
            // start processes (starts timer task to do the analysis)
            startFirstWorkflow(analysis);

            output.put("status", "Analysis is started");
            output.put("message", "App is now a subscriber");

        } else {
            log.debug("Analysis is currently running");
            appSubscribers = startedAnalyses.get(analysisId);

            output.put("status", "Analysis is already running.");

            // If the analysis is started check if the app is a subscriber
            if (!appSubscribers.contains(appKey)) {
                appSubscribers.add(appKey);
                output.put("message", "Added app to subscribers list");
            } else {
                output.put("message", "This app is already a subscriber");
            }
        }

        return output;
    }

    /**
     * Pulls the targetSystem from the spc and puts it into the (transient)
     * analysis.
     * <p>
     * If pulled target system has no ontology path, it is not ready and this method
     * throws a {@link TargetSystemNotReadyException}.
     *
     * @param analysis
     */
    private void fillAnalysisWithTargetSystem(Analysis analysis) {
        Long targetSystemId = analysis.getTargetSystemId();
        TargetSystem targetSystem = smeClientService.getTargetSystem(targetSystemId);
        if (targetSystem.getOntologyPath() != null) {
            analysis.setTargetSystem(targetSystem);
        } else {
            throw new TargetSystemNotReadyException(targetSystemId);
        }
    }

    private App getApp(String appKey) {
        return appRepository.findById(appKey).orElseThrow(() -> {
            log.warn("No app with key " + appKey + " found.");
            return new ResourceNotFoundException("app", appKey);
        });
    }

    private Analysis getAnalysis(Long analysisId) {
        return analysisRepository.findById(analysisId).orElseThrow(() -> {
            log.warn("No analysis with id " + analysisId + " found.");
            return new ResourceNotFoundException("analysis", analysisId);
        });
    }

    @SuppressWarnings("unchecked")
    private void loadPluginRunner(AnalysisPlugin analysisPlugin) {
        if (this.loadedPlugins.containsKey(analysisPlugin.getPluginFileName()))
            return;
        File pluginsDir = new File("plugins");
        if (!pluginsDir.isDirectory())
            log.warn(pluginsDir.getAbsoluteFile() + " is not a directory! Directory needed as plugin directory.");
        File pluginFile = null;
        for (File plugin : pluginsDir.listFiles()) {
            if (plugin.getName().equals(analysisPlugin.getPluginFileName())) {
                pluginFile = plugin;
            }
        }
        if (pluginFile == null) {
            throw new NotFoundException(
                    "Plugin " + analysisPlugin.getPluginFileName() + " not found in " + pluginsDir.getAbsolutePath());
        }
        try {
            ClassLoader loader = URLClassLoader.newInstance(new URL[] { pluginFile.toURL() },
                    getClass().getClassLoader());
            Class<?> clazz = Class.forName(analysisPlugin.getPluginClasspath(), true, loader);
            Class<? extends AnalysisPluginAbstract> analysisPluginClass = clazz
                    .asSubclass(AnalysisPluginAbstract.class);
            Class<?> resultClazz = Class.forName(analysisPlugin.getResultClasspath(), true, loader);
            Class<? extends AnalysisReportAbstract> analysisResultClass = resultClazz
                    .asSubclass(AnalysisReportAbstract.class);

            // Add new entry for plugin identified by its filename
            this.loadedPlugins.put(analysisPlugin.getPluginFileName(), new HashMap<>());

            // add loaded
            this.loadedPlugins.get(analysisPlugin.getPluginFileName()).put(analysisPlugin.getPluginClasspath(),
                    analysisPluginClass);
            this.loadedPlugins.get(analysisPlugin.getPluginFileName()).put(analysisPlugin.getResultClasspath(),
                    analysisResultClass);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Throws UnsatisfiableException if the satisfiable check fails.
     * <p>
     * Checks for:
     * <ul>
     * <li>if docker is available</li>
     * </ul>
     *
     * @param analysis
     * @throws UnsatisfiableException
     */
    private void checkSatisfiability(Analysis analysis) throws UnsatisfiableException {
        try {
            if (!workerService.isAvailable()) {
                throw new UnsatisfiableException("Docker Service not available");
            }
        } catch (WorkerServiceNotAvailableException wsex) {
            log.warn("Docker Service not available");
            throw new UnsatisfiableException("Docker Service not available", wsex);
        }
        // is camunda available
        // is fuseki available
        // is spc available

        // are containers deployed? if not deploy
    }

    public void reportError(Long analysisId, Exception ex) {
        AnalysisState analysisState = runningAnalyses.get(analysisId);
        Analysis analysis = analysisState.getAnalysis();
        // TODO remove deployment?
        // TODO add error as report?
        analysis.getCurrentAnalysisReport().setError(ex.getMessage());
        finishAnalysis(analysis);
    }

    private void startFirstWorkflow(Analysis analysis) throws InterruptedException {
        AnalysisState analysisState = new AnalysisState(analysis);
        runningAnalyses.put(analysis.getId(), analysisState);
        startWorkflow(analysisState);
    }

    private synchronized void startWorkflow(AnalysisState analysisState) throws InterruptedException {
        Workflow workflow = analysisState.getCurrentPhaseWorkflow();
        CamundaProcessInstance processInstance = camundaClient
                .startCamundaProcess(workflow.getCamundaProcessDefinitionId());
        analysisState.setStartedProcessInstance(processInstance);
        asyncService.camundaProcessMonitor(analysisState.getAnalysis().getId(), processInstance.getId(), 5000);
    }

    public void createCamundaDeployment(Analysis analysis) {
        String deploymentName = analysis.getName() + "_Deployment";
        List<Workflow> workflows = analysis.getWorkflows();
        Map<Workflow, String> workflowDefinitionIdMap = camundaClient.deployWorkflows(workflows, deploymentName);
        workflowDefinitionIdMap.forEach(((workflow, camundaProcessDefinitionId) -> workflow
                .setCamundaProcessDefinitionId(camundaProcessDefinitionId)));
        // updating analysis
        analysisRepository.save(analysis);
    }

    public synchronized void completedProcess(Long analysisId, String processInstanceId,
            CamundaProcessInstanceHistory history) throws InterruptedException {
        log.info("Completed process {}", processInstanceId);
        AnalysisState analysisState = runningAnalyses.get(analysisId);
        // move to next phase
        analysisState.completedProcess(processInstanceId, history);
        // start next workflow if not finished
        if (analysisState.isFinished()) {
            log.info("Analysis {}: all phases finished", analysisId);
            runQueriesForAnalysis(analysisState.getAnalysis(), analysisState.getOutputOntologyName());
        } else {
            startWorkflow(analysisState);
        }
    }

    public synchronized void suspendedProcess(Long analysisId, String processInstanceId,
            CamundaProcessInstanceHistory history) {
        log.info("Suspended process {}", processInstanceId);
        AnalysisState analysisState = runningAnalyses.get(analysisId);
        reportError(analysisId,
                new CamundaProcessSuspendedException("Process " + processInstanceId + " got suspended"));
    }

    private synchronized void runQueryForPolicyAnalysis(PolicyAnalysis policyAnalysis, String outputOntologyName) {
        AnalysisPlugin plugin = policyAnalysis.getAnalysisPlugin();
        // Run the actual analysis
        Map<String, Object> pluginClasses = loadedPlugins.get(plugin.getPluginFileName());
        // load jar in sandbox
        // run analysis plugin (AnalysisPluginInterface needed) using its arguments
        log.info("Running query for policyAnalysis {}.", policyAnalysis.getName());

        @SuppressWarnings("unchecked")
        Class<? extends AnalysisPluginAbstract> analysisPluginClass = (Class<? extends AnalysisPluginAbstract>) pluginClasses
                .get(plugin.getPluginClasspath());
        String analysisPluginParameters = policyAnalysis.getQuery();
        try {
            analysisPluginParameters = URLDecoder.decode(analysisPluginParameters, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error("Analysis plugin arguments could not be decoded");
            throw new AnalysisFailedException("Analysis plugin arguments could not be decoded", e);
        }
        try {
            AnalysisPluginAbstract analysisPlugin = analysisPluginClass.newInstance();
            analysisPlugin.initialize(ftpClientService.getUriFor(outputOntologyName), analysisPluginParameters);
            Thread pluginThread = new Thread(analysisPlugin);
            pluginThread.start();
            pluginThread.join();
            AnalysisReportAbstract result = analysisPlugin.getResult();
            PolicyAnalysisReport policyAnalysisReport = new PolicyAnalysisReport(result.getReportJsonString());
            policyAnalysisReport.setAnalysisReport(policyAnalysis.getAnalysis().getCurrentAnalysisReport());
            policyAnalysis.addPolicyAnalysisReport(policyAnalysisReport);
            policyAnalysisReportRepository.save(policyAnalysisReport);
            policyAnalysisRepository.save(policyAnalysis);
        } catch (InstantiationException e) {
            log.error("Unable to instantiate analysis plugin {}", plugin.getPluginFileName());
            throw new AnalysisFailedException("Unable to instantiate analysis plugin " + plugin.getPluginFileName(), e);
        } catch (IllegalAccessException e) {
            log.error("Illegal access observed while instantiating analysis plugin {}", plugin.getPluginFileName());
            throw new AnalysisFailedException(
                    "Illegal access observed while instantiating analysis plugin " + plugin.getPluginFileName(), e);
        } catch (InterruptedException e) {
            log.error("Plugin thread got interrupted for plugin {}", plugin.getPluginFileName());
            throw new AnalysisFailedException("Plugin thread got interrupted for plugin " + plugin.getPluginFileName(),
                    e);
        }
    }

    public synchronized void runQueriesForAnalysis(Analysis analysis, String outputOntologyName) {
        // run query for each policy analysis
        for (PolicyAnalysis policyAnalysis : analysis.getPolicyAnalyses()) {
            // loading plugin runner
            loadPluginRunner(policyAnalysis.getAnalysisPlugin());
            // run the query and generate a report
            runQueryForPolicyAnalysis(policyAnalysis, outputOntologyName);
        }
        finishAnalysis(analysis);
    }

    public synchronized void finishAnalysis(Analysis analysis) {
        // remove analysis state
        runningAnalyses.remove(analysis.getId());

        // remove the report as current analysis report
        AnalysisReport analysisReport = analysis.getCurrentAnalysisReport();
        analysis.setCurrentAnalysisReport(null);
        // add finish time to report and save it
        Timestamp finishTimestamp = new Timestamp(System.currentTimeMillis());
        analysisReport.setFinishTime(finishTimestamp);
        analysisReportRepository.save(analysisReport);
        analysis.setLastFinish(finishTimestamp);

        // Notify subscribers
        List<String> subscribers = startedAnalyses.get(analysis.getId());
        for (String appKey : subscribers) {
            App app = getApp(appKey);
            log.info("Notified app {} about new report with id {}", app.getKey(), analysisReport.getId());
            notify(app.getReportCallbackURI(), app.getToken(), analysis.getUuid(), analysisReport.getId());
        }
        analysisRepository.save(analysis);

        startedAnalyses.remove(analysis.getId());
        log.info("Finished Analysis {} with id {}", analysis.getName(), analysis.getId());
    }

    private synchronized void notify(String reportCallbackURI, String token, String analysisUuid, Long reportId) {
        RestTemplate restTemplate;
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        HttpEntity<ReportNotification> requestEntity = new HttpEntity<ReportNotification>(
                new ReportNotification(analysisUuid, reportId), headers);

        try {
            restTemplate = restTemplateCreator.create();
            restTemplate.postForLocation(reportCallbackURI, requestEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized CamundaProcessInstanceHistory getCurrentCamundaProcessInstanceHistory(
            String processInstanceId) {
        return camundaClient.getCamundaProcessInstanceHistory(processInstanceId);
    }
}
