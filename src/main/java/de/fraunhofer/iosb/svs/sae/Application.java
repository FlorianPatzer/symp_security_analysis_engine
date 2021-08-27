package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.db.*;
import de.fraunhofer.iosb.svs.sae.security.ReloadableX509TrustManager;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.github.jsonldjava.shaded.com.google.common.io.Resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
@EntityScan(basePackages = { "de.fraunhofer.iosb.svs.sae.db" })
@EnableAsync
public class Application extends SpringBootServletInitializer implements CommandLineRunner {

    private final AppRepository appRepository;
    private final AnalysisRepository analysisRepository;
    private final PolicyAnalysisRepository policyAnalysisRepository;

    @Autowired
    public Application(AppRepository appRepository, AnalysisRepository analysisRepository,
            PolicyAnalysisRepository policyAnalysisRepository) {
        this.appRepository = appRepository;
        this.analysisRepository = analysisRepository;
        this.policyAnalysisRepository = policyAnalysisRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Value("${truststore.custom.path}")
    private String customTrustStorePath;

    @Value("${truststore.custom.password}")
    private String customTrustStorePassword;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStart() throws URISyntaxException {
        String absoluteFilePath = Resources.getResource(customTrustStorePath).getPath();
        ReloadableX509TrustManager.init(absoluteFilePath, customTrustStorePassword);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        // filter to show only id
        filterProvider.addFilter("idOnly", SimpleBeanPropertyFilter.filterOutAllExcept("id"));
        filterProvider.addFilter("nameOnly", SimpleBeanPropertyFilter.filterOutAllExcept("name"));
        filterProvider.addFilter("shortPolicyAnalysis",
                SimpleBeanPropertyFilter.filterOutAllExcept("name", "description", "link", "query"));
        return builder -> builder.filters(filterProvider);
    }

    @Override
    public void run(String... args) throws Exception {
    }
}
