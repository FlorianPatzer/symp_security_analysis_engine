package de.fraunhofer.iosb.svs.sae;

import de.fraunhofer.iosb.svs.sae.db.*;
import de.fraunhofer.iosb.svs.sae.dto.AnalysisDTO;
import de.fraunhofer.iosb.svs.sae.security.JWTMisc;
import de.fraunhofer.iosb.svs.sae.security.RestTemplateWithoutCAValidation;
import okhttp3.mockwebserver.MockWebServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.javatuples.Pair;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnalysisTests {
    private static final Logger log = LoggerFactory.getLogger(AnalysisTests.class);

    private static String analysisUuid = "";
    private static String analysisName = "Test Analysis";
    private static String analysisDescription = "Test Description";
    private static List<String> policyAnalyses = new ArrayList<String>();
    private static MockWebServer mockClient;

    private static String appKey = "Mock App";
    private static boolean appActive = true;
    private static String appReportCallbackURI = "";
    private static String tokenFromApp = "not present";
    private static String appCertificate = "";
    private static String appJwt = "";

    @LocalServerPort
    private int port;

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private AnalysisRepository analysisRepository;
    @Autowired
    private AppRepository appRepository;

    private static RestTemplate restTemplate;
    
    @Value("${spring.profiles.active}")
    String activeProfile;

    @Value("${http.protocol}")
    String httpProtocol;

    @Value("${ah.endpoint}")
    String ahBaseUrl;

    @Value("${tests.ah.policyimplementation}")
    String testPolicyImplementation;

    @Value("${tests.spc.targetsystemId}")
    String testTargetSystemId;

    @BeforeAll
    static void setUp() throws Exception {
        restTemplate = RestTemplateWithoutCAValidation.create();
        mockClient = new MockWebServer();
        mockClient.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockClient.shutdown();
    }

    @Test
    @Order(1)
    public void registerMockApp() throws IOException {
        log.info("Testing app registration");
        // Read certificate and encode it with base64
        ClassLoader classLoader = getClass().getClassLoader();
        File certFile = new File(classLoader.getResource("ssl_dev/cert.crt").getFile());
        byte[] fileBytes = FileUtils.readFileToByteArray(certFile);
        byte[] encodedBytes = Base64.getEncoder().encode(fileBytes);
        String file_base64_string = new String(encodedBytes, "UTF8");
        appCertificate = file_base64_string;

        App app = new App(appKey, appActive, true, appReportCallbackURI, appCertificate, tokenFromApp);

        appReportCallbackURI = mockClient.url("/").toString();

        log.debug("Mock app report url is {}", appReportCallbackURI);

        // Check if the app already exists and delete it
        if (appRepository.existsById(app.getKey())) {
            appRepository.delete(app);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", "");

        ObjectMapper mapper = new ObjectMapper();
        String requestData = mapper.writeValueAsString(app);

        HttpEntity<String> request = new HttpEntity<String>(requestData, headers);
        ResponseEntity<String> response = restTemplate
                .postForEntity(httpProtocol + "://localhost:" + port + "/api/v1/app/register", request, String.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> bodyJsonToMap = new ObjectMapper().readValue(response.getBody(), Map.class);
        String tokenFromEngine = (String) bodyJsonToMap.get("token"); // here you get the parameters

        appJwt = tokenFromEngine;

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(tokenFromEngine, not(""));
        assertThat(appRepository.exists(Example.of(app)), is(true));
    }

    @Test
    @Order(2)
    public void testAddingAnalysis() throws JsonProcessingException {
        analysisUuid = UUID.randomUUID().toString();
        
        String policy_suffix = "";
        
        if(activeProfile.equals("k8s") || activeProfile.equals("gitlab")) {
            policy_suffix = "_K8S";
        }

        policyAnalyses.add(ahBaseUrl + "/api/v1/policyimplementation/" + testPolicyImplementation + policy_suffix);

        long targetSystemId = Long.valueOf(testTargetSystemId);
        AnalysisDTO analysisDTO = new AnalysisDTO(analysisUuid, analysisName, analysisDescription, targetSystemId,
                policyAnalyses);

        // Check if the analysis already exists and delete it
        if (analysisRepository.existsByUuid(analysisUuid)) {
            analysisService.deleteByUUID(analysisUuid);
        }

        // Create Request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", appJwt);

        ObjectMapper mapper = new ObjectMapper();
        String requestData = mapper.writeValueAsString(analysisDTO);

        // Make request
        HttpEntity<String> request = new HttpEntity<String>(requestData, headers);
        ResponseEntity<String> response = restTemplate
                .postForEntity(httpProtocol + "://localhost:" + port + "/api/v1/analysis", request, String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(analysisRepository.existsByUuid(analysisUuid), is(true));
    }

    @Test
    @Order(3)
    public void testGettingRegisteredAnalysis() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", appJwt);

        HttpEntity<?> entity = new HttpEntity<Object>(headers);

        ResponseEntity<Analysis> response = restTemplate.exchange(
                httpProtocol + "://localhost:" + port + "/api/v1/analysis/" + analysisUuid, HttpMethod.GET, entity,
                Analysis.class);

        Analysis analysis = response.getBody();
        assertThat(analysis.getDescription(), is(analysisDescription));
    }

    @Test
    @Order(4)
    public void testGettingAllRegisteredAnalyses() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", appJwt);

        HttpEntity<?> entity = new HttpEntity<Object>(headers);

        ResponseEntity<List<Analysis>> response = restTemplate.exchange(
                httpProtocol + "://localhost:" + port + "/api/v1/analysis/", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<Analysis>>() {
                });

        List<Analysis> analyses = response.getBody();
        assertThat(analyses.size(), not(0));
    }

    @Test
    @Order(5)
    public void testStartingAnalysis() {
        // Create Request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", appJwt);

        String requestData = "{}";

        // Make request
        HttpEntity<String> request = new HttpEntity<String>(requestData, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                httpProtocol + "://localhost:" + port + "/api/v1/analysis/" + analysisUuid + "/start", request,
                String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @Order(6)
    public void testGettingAnalysisStatus() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", appJwt);

        HttpEntity<?> entity = new HttpEntity<Object>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                httpProtocol + "://localhost:" + port + "/api/v1/analysis/" + analysisUuid + "/status", HttpMethod.GET,
                entity, String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }
    

    @Test
    @Order(8)
    public void deleteMockApp() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", appJwt);

        HttpEntity<String> request = new HttpEntity<String>("", headers);

        ResponseEntity<String> response = restTemplate
                .postForEntity(httpProtocol + "://localhost:" + port + "/api/v1/app/unregister", request, String.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(appRepository.existsById(appKey), is(false));
    }
}
