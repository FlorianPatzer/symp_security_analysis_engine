package de.fraunhofer.iosb.svs.sae.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionAdvice {

    @ResponseBody
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(UnsatisfiableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleUnsatisfiableRequirement(UnsatisfiableException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleNumberFormat(NumberFormatException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(AnalysisFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleAnalysisFailed(AnalysisFailedException ex) {
        return ex.getAnswerMessage();
    }

    @ResponseBody
    @ExceptionHandler(AnalysisAlreadyRunningException.class)
    @ResponseStatus(HttpStatus.OK)
    public String handleAnalysisAlreadyRunning(AnalysisAlreadyRunningException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(InvalidUrlException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidUrl(InvalidUrlException ex) {
        return ex.getMessage();
    }

    @ResponseBody
    @ExceptionHandler(AnalysisCreationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleAnalysisCreation(AnalysisCreationException ex) {
        return ex.getMessage() + " because of " + ex.getCause().getMessage();
    }

}
