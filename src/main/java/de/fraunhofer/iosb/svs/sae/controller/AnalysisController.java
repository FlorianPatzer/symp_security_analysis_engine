package de.fraunhofer.iosb.svs.sae.controller;

import de.fraunhofer.iosb.svs.sae.MainEngineService;
import de.fraunhofer.iosb.svs.sae.configuration.RestApiV1Controller;
import de.fraunhofer.iosb.svs.sae.db.Analysis;
import de.fraunhofer.iosb.svs.sae.db.AnalysisService;
import de.fraunhofer.iosb.svs.sae.dto.AnalysisDTO;
import de.fraunhofer.iosb.svs.sae.exceptions.AnalysisAlreadyRunningException;
import de.fraunhofer.iosb.svs.sae.security.JWTMisc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping(path = "/analysis/{uuid}", produces = "application/json")
    public ResponseEntity<Analysis> getAnalysisByUUID(@RequestHeader("token") String token,
            @PathVariable("uuid") String uuid) {
        // TODO Decide if only authenticated apps that created the analysis should have
        // rights to get the analysis data
        return ResponseEntity.ok(analysisService.findByUUID(uuid));
    }

    @PostMapping(path = "/analysis", produces = "application/json")
    public ResponseEntity<?> addAnalysis(@RequestHeader("token") String token,
            @RequestBody AnalysisDTO analysisDTO) throws Exception {
        DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
        String key = jwt.getClaim("name").asString();

        ResponseEntity<?> response;

        if (analysisDTO.getPolicyAnalyses().size() > 0) {
            Analysis savedAnalysis = analysisService.addAnalysis(key, analysisDTO);
            response = new ResponseEntity<>(getUriForUUID(savedAnalysis.getUuid()), HttpStatus.OK);
        } else {
            Map<String, String> responseData = new HashMap<String, String>();
            responseData.put("status","Analysis must contain at least one policy.");
            response = new ResponseEntity<>(responseData, HttpStatus.BAD_REQUEST);
        }

        return response;
    }

    @DeleteMapping(path = "/analysis/{uuid}")
    public ResponseEntity<?> deleteAnalysis(@RequestHeader("token") String token, @PathVariable("uuid") String uuid) {
        Analysis analysis = analysisService.findByUUID(uuid);
        analysisService.deleteById(analysis.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(path = "/analysis", produces = "application/json")
    public Analysis updateAnalysis(@RequestHeader("key") String key, @RequestBody Analysis analysis) {
        throw new UnsupportedOperationException("Update not supported");
        // return analysisRepository.save(analysis);
    }

    @GetMapping(path = "/analysis/{uuid}/status", produces = "application/json")
    public ResponseEntity<Map<String, String>> checkAnalysis(@RequestHeader("token") String token,
            @PathVariable("uuid") String uuid) throws InterruptedException {
        Analysis analysis = analysisService.findByUUID(uuid);

        Map<String, String> data = new HashMap<String, String>();
        ResponseEntity<Map<String, String>> response;

        if (analysis != null) {
            data = engine.checkAnalysisStatus(analysis);
            response = new ResponseEntity<Map<String, String>>(data, HttpStatus.OK);
        } else {
            data.put("status", "No analysis with uuid:" + uuid);
            response = new ResponseEntity<Map<String, String>>(data, HttpStatus.BAD_REQUEST);
        }

        return response;
    }

    @PostMapping("/analysis/{uuid}/start")
    public ResponseEntity<Map<String, String>> runAnalysis(@RequestHeader("token") String token,
            @PathVariable("uuid") String uuid) throws InterruptedException {
        DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
        String key = jwt.getClaim("name").asString();

        Analysis analysis = analysisService.findByUUID(uuid);

        Map<String, String> data = new HashMap<String, String>();
        ResponseEntity<Map<String, String>> response;

        if (analysis != null) {
            data = engine.startAnalysis(analysis, key);
            response = new ResponseEntity<Map<String, String>>(data, HttpStatus.OK);
        } else {
            data.put("status", "No analysis with uuid:" + uuid);
            response = new ResponseEntity<Map<String, String>>(data, HttpStatus.BAD_REQUEST);
        }

        return response;
    }

    private URI getUriForUUID(String uuid) {
        Analysis analysis = analysisService.findByUUID(uuid);

        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(analysis.getId()).toUri();
    }
}
