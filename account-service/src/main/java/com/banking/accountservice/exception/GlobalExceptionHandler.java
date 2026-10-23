package com.banking.accountservice.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle 400, 404, 409 from ServiceImpl
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleStatus(
            ResponseStatusException ex) {

        return ResponseEntity.status(ex.getStatusCode())
                .body(ex.getReason());
    }

    // Handle @Valid validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

        return ResponseEntity.badRequest().body(message);
    }

    // Handle invalid request data
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<String> handleBadRequest(Exception ex) {

        return ResponseEntity.badRequest()
                .body("Invalid request data");
    }

    // Handle duplicate database values
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDatabase(
            DataIntegrityViolationException ex) {

        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof SQLException sql &&
                "23505".equals(sql.getSQLState())) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Duplicate account data");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Database operation failed");
    }

    // Handle concurrent updates
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<String> handleConcurrency(
            OptimisticLockingFailureException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Account was modified by another request");
    }

    // Handle wrong HTTP methods
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethod(
            HttpRequestMethodNotSupportedException ex) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("HTTP method not allowed");
    }

    // Handle invalid API paths
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNotFound(
            NoResourceFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("API not found");
    }

    // Handle unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Something went wrong");
    }
}