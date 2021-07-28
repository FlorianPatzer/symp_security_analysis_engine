package de.fraunhofer.iosb.svs.sae.plugin;


public interface AnalysisPluginAbstract extends Runnable {
    /**
     * Loads the plugin with the given arguments before letting it run.
     *
     * @param inputOntologyPath
     * @param arguments
     */
    public void initialize(String inputOntologyPath, String arguments);

    @Override
    public void run();

    public AnalysisReportAbstract getResult();

}
