package com.cgaraydev.incidentapi.error;

import com.cgaraydev.incidentapi.incident.IncidentNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import com.cgaraydev.incidentapi.incident.InvalidIncidentStatusTransitionException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    public ProblemDetail handleIncidentNotFound(
            IncidentNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage());

        problem.setTitle("Incident not found");

        return problem;
    }

    @ExceptionHandler(InvalidIncidentStatusTransitionException.class)
    public ProblemDetail handleInvalidStatusTransition(
            InvalidIncidentStatusTransitionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage());

        problem.setTitle("Invalid status transition");

        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldValidationError(
                        error.getField(),
                        error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                "One or more fields are invalid.");

        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);

        return handleExceptionInternal(
                exception,
                problem,
                headers,
                status,
                request);
    }

    public record FieldValidationError(String field, String message) {
    }
}