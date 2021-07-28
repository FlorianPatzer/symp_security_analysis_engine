package de.fraunhofer.iosb.svs.sae.controller;

import de.fraunhofer.iosb.svs.sae.ReportService;
import de.fraunhofer.iosb.svs.sae.configuration.RestApiV1Controller;
import de.fraunhofer.iosb.svs.sae.db.AnalysisReport;
import de.fraunhofer.iosb.svs.sae.security.JWTMisc;

import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;

@RestApiV1Controller
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(path = "/report/{id}", produces = "application/json")
    public ResponseEntity<?> getAnalysisReportById(@RequestHeader("token") String token, @PathVariable("id") Long id) {        
        HashMap<String, String> data = new HashMap<>();
        HttpStatus status;
        DecodedJWT jwt = JWTMisc.verifyAndDecode(token);
        
        ResponseEntity<?> responseEntity = null;
        if (jwt != null) {
            AnalysisReport analysisReport = reportService.getAnalysisReportById(id);
            responseEntity = ResponseEntity.ok(analysisReport);
        }
        else {
            data.put("status", "Invalid token");
            status = HttpStatus.BAD_REQUEST;
            responseEntity = new ResponseEntity<HashMap<String, String>>(data, status);
        }
        
        return responseEntity;
    }
}
