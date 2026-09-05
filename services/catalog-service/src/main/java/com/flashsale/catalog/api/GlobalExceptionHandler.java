package com.flashsale.catalog.api;

import com.flashsale.catalog.product.DuplicateSkuException;
import com.flashsale.catalog.product.ProductNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail notFound(ProductNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Product unavailable", ex.getMessage());
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ProblemDetail duplicate(DuplicateSkuException ex) {
        ProblemDetail detail = problem(HttpStatus.CONFLICT, "SKU already exists", ex.getMessage());
        detail.setProperty("errors", Map.of("sku", "A product with this SKU already exists"));
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail invalidParameters(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            errors.put(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        });
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Check the request parameters.");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail unavailable(DataAccessException ex) {
        log.error("Catalog database operation failed", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Catalog temporarily unavailable", "Please try again shortly.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Check the highlighted product fields.");
        detail.setProperty("errors", errors);
        return handleExceptionInternal(ex, detail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, "Invalid request body",
                "Provide valid JSON with the expected field types."), headers, HttpStatus.BAD_REQUEST, request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        return detail;
    }
}
