package de.fraunhofer.iosb.svs.sae;

import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.svs.sae.db.App;
import de.fraunhofer.iosb.svs.sae.db.AppRepository;
import de.fraunhofer.iosb.svs.sae.security.RestTemplateWithoutCAValidation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.util.Base64;
import java.util.Map;

import org.apache.commons.io.FileUtils;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppTests {

    @LocalServerPort
    private int port;

    @Value("${http.protocol}")
    String httpProtocol;

    @Autowired
    private AppRepository appRepository;

    private static final Logger log = LoggerFactory.getLogger(AppTests.class);

    private static RestTemplate restTemplate;
    private static String appJwt;
    
    private static String appKey = "Test App";
    private static boolean appActive = true;
    private static String appReportCallbackURI = "not present";
    private static String tokenFromApp = "not present";
    private static String appCertificate = "";

    @BeforeAll
    public static void setup() throws Exception {
        restTemplate = RestTemplateWithoutCAValidation.create();
    }

    @Test
    @Order(1)
    public void testAddingApp() throws Exception {
        log.info("Testing app registration");
        // Read certificate and encode it with base64
        ClassLoader classLoader = getClass().getClassLoader();
        File certFile = new File(classLoader.getResource("ssl/cert.crt").getFile());
        byte[] fileBytes = FileUtils.readFileToByteArray(certFile);
        byte[] encodedBytes = Base64.getEncoder().encode(fileBytes);
        String file_base64_string = new String(encodedBytes, "UTF8");
        appCertificate = file_base64_string;

        App app = new App(appKey, appActive, true, appReportCallbackURI, appCertificate, tokenFromApp);

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
    public void testActivateDeactivate() {
        log.info("Testing app activation and deactivation");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", appJwt);
        
        HttpEntity<String> request = new HttpEntity<String>("", headers);

        ResponseEntity<String> response;

        response = restTemplate.postForEntity(httpProtocol + "://localhost:" + port + "/api/v1/app/activate", request,
                String.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        
        
        response = restTemplate
                .postForEntity(httpProtocol + "://localhost:" + port + "/api/v1/app/deactivate", request, String.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @Order(3)
    public void testGetAppStatus() {
        log.info("Testing app status check");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("token", appJwt);

        HttpEntity<?> entity = new HttpEntity<Object>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(httpProtocol + "://localhost:" + port + "/api/v1/app",
                HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }
    
    @Test
    @Order(4)
    public void testAppUnregisterAndDelete() {
        log.info("Testing app unregistration");
        
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
