package de.fraunhofer.iosb.svs.sae.controller;

import de.fraunhofer.iosb.svs.sae.MainEngineService;
import de.fraunhofer.iosb.svs.sae.configuration.RestApiV1Controller;
import de.fraunhofer.iosb.svs.sae.db.Analysis;
import de.fraunhofer.iosb.svs.sae.db.AnalysisRepository;
import de.fraunhofer.iosb.svs.sae.db.AnalysisService;
import de.fraunhofer.iosb.svs.sae.db.App;
import de.fraunhofer.iosb.svs.sae.db.AppRepository;
import de.fraunhofer.iosb.svs.sae.dto.AnalysisDTO;
import de.fraunhofer.iosb.svs.sae.exceptions.ResourceAlreadyExistsException;
import de.fraunhofer.iosb.svs.sae.security.JWTMisc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.hash.Hashing;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@RestApiV1Controller
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);
    private final AnalysisService analysisService;
    private final MainEngineService engine;

    @Autowired
    public AnalysisController(AnalysisService analysisService, MainEngineService engine) {
        this.analysisService = analysisService;
        this.engine = engine;
    }

    @GetMapping(path = "/analysis", produces = "application/json")
    public List<Analysis> getAnalysis(@RequestHeader("token") String token) {
        DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
        String key = jwt.getClaim("name").asString();
        return analysisService.findAll(key);
    }

    @GetMapping(path = "/analysis/{id}", produces = "application/json")
    public ResponseEntity<Analysis> getAnalysisByhash(@RequestHeader("token") String token,
            @PathVariable("id") Long analysisId) {
        // TODO Decide if only authenticated apps that created the analysis should have
        // rights to get the analysis data
        return ResponseEntity.ok(analysisService.findById(analysisId));
    }

    @PostMapping(path = "/analysis", produces = "application/json")
    public ResponseEntity<?> addAnalysis(@RequestHeader("token") String token, @RequestBody AnalysisDTO analysisDTO)
            throws Exception {
        DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
        String key = jwt.getClaim("name").asString();

        ResponseEntity<?> response;

        List<String> analysisPolicies = analysisDTO.getPolicyAnalyses();
        Long targetSystemId = analysisDTO.getTargetSystemId();

        String analysisHash = analysisService.createHash(analysisPolicies, targetSystemId);

        Map<String, String> responseData = new HashMap<>();

        if (analysisPolicies.size() > 0) {
            // Hash check is done in the addAnalysis method, which throws the
            // ResourceAlreadyExistsException
            try {
                Analysis createdAnalysis = analysisService.addAnalysis(key, analysisDTO, analysisHash);
                responseData.put("status", "Analysis created successfully.");
                responseData.put("analysisId", createdAnalysis.getId().toString());
                response = new ResponseEntity<Map<String, String>>(responseData, HttpStatus.OK);
            } catch (ResourceAlreadyExistsException e) {
                Analysis existingAnalysis = analysisService.findByHash(analysisHash);
                responseData.put("status", "Analysis with the same policies already exists.");
                responseData.put("analysisId", existingAnalysis.getId().toString());
                response = new ResponseEntity<Map<String, String>>(responseData, HttpStatus.CONFLICT);

            }
        } else {
            responseData.put("status", "Analysis must contain at least one policy.");
            response = new ResponseEntity<>(responseData, HttpStatus.BAD_REQUEST);
        }

        return response;
    }

    @PostMapping(path = "/analysis/{id}/subscribe", produces = "application/json")
    public ResponseEntity<Map<String, String>> subscribeToAnalysis(@RequestHeader("token") String token,
            @PathVariable("id") Long analysisId) throws InterruptedException, JsonProcessingException {
        DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
        String appKey = jwt.getClaim("name").asString();

        Map<String, String> data = new HashMap<String, String>();
        ResponseEntity<Map<String, String>> response;

        Analysis analysis = analysisService.findById(analysisId);

        SimpleFilterProvider filters = new SimpleFilterProvider();
        filters.addFilter("idOnly", SimpleBeanPropertyFilter.filterOutAllExcept("id"));
        filters.addFilter("nameOnly", SimpleBeanPropertyFilter.filterOutAllExcept("name"));
        filters.addFilter("shortPolicyAnalysis",
                SimpleBeanPropertyFilter.filterOutAllExcept("name", "description", "link", "query"));
        ObjectMapper mapper = new ObjectMapper().setFilterProvider(filters);

        String analysisJsonString = mapper.writeValueAsString(analysis);

        String message = engine.addSubscriberToAnalysis(analysisId, appKey);

        data.put("status", message);
        data.put("analysis", analysisJsonString);
        response = new ResponseEntity<Map<String, String>>(data, HttpStatus.OK);

        return response;
    }

    @PostMapping(path = "/analysis/{id}/unsubscribe", produces = "application/json")
    public ResponseEntity<Map<String, String>> unsubscribeFromAnalysis(@RequestHeader("token") String token,
            @PathVariable("id") Long analysisId) throws InterruptedException {
        DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
        String appKey = jwt.getClaim("name").asString();

        Map<String, String> data = new HashMap<String, String>();
        ResponseEntity<Map<String, String>> response;

        String message = engine.removeSubscriberFromAnalysis(analysisId, appKey);

        data.put("status", message);
        response = new ResponseEntity<Map<String, String>>(data, HttpStatus.OK);

        return response;
    }

    @DeleteMapping(path = "/analysis/{id}")
    public ResponseEntity<?> deleteAnalysis(@RequestHeader("token") String token, @PathVariable("id") Long analysisId) {
        analysisService.deleteById(analysisId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(path = "/analysis", produces = "application/json")
    public Analysis updateAnalysis(@RequestHeader("token") String token, @RequestBody Analysis analysis) {
        // TODO: Implement
        throw new UnsupportedOperationException("Update not supported");
        // return analysisRepository.save(analysis);
    }

    @GetMapping(path = "/analysis/{id}/status", produces = "application/json")
    public ResponseEntity<Map<String, String>> checkAnalysis(@RequestHeader("token") String token,
            @PathVariable("id") Long id) throws InterruptedException {
        Analysis analysis = analysisService.findById(id);

        Map<String, String> data = new HashMap<String, String>();
        ResponseEntity<Map<String, String>> response;

        if (analysis != null) {
            data = engine.checkAnalysisStatus(analysis);
            response = new ResponseEntity<Map<String, String>>(data, HttpStatus.OK);
        } else {
            data.put("status", "No analysis with id:" + id.toString());
            response = new ResponseEntity<Map<String, String>>(data, HttpStatus.BAD_REQUEST);
        }

        return response;
    }

    @PostMapping("/analysis/{id}/start")
    public ResponseEntity<Map<String, String>> runAnalysis(@RequestHeader("token") String token,
            @PathVariable("id") Long analysisId) throws InterruptedException {
        DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
        String appKey = jwt.getClaim("name").asString();

        Analysis analysis = analysisService.findById(analysisId);

        Map<String, String> data = new HashMap<String, String>();
        ResponseEntity<Map<String, String>> response;

        if (analysis != null) {
            data = engine.startAnalysis(analysisId, appKey);
            response = new ResponseEntity<Map<String, String>>(data, HttpStatus.OK);
        } else {
            data.put("status", "No analysis with id:" + analysisId.toString());
            response = new ResponseEntity<Map<String, String>>(data, HttpStatus.BAD_REQUEST);
        }

        return response;
    }

    private URI getUriForHash(String analysisHash) {
        Analysis analysis = analysisService.findByHash(analysisHash);
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(analysis.getId()).toUri();
    }
}
