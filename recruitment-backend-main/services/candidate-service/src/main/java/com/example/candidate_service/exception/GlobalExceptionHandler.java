package com.example.candidate_service.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.candidate_service.dto.Response;
import com.fasterxml.jackson.databind.JsonNode;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Object>> handleAllException(Exception ex) {
        Response<Object> res = new Response<Object>();
        res.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        res.setMessage(ex.getMessage());
        res.setError("Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    @ExceptionHandler(value = {
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            MissingRequestCookieException.class,
            IdInvalidException.class
    })
    public ResponseEntity<Response<Object>> handleException(Exception ex) {
        Response<Object> response = new Response<Object>();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setMessage(ex.getMessage());
        response.setError("Ngoại lệ xảy ra");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<Response<Object>> handleNotFoundException(NoResourceFoundException ex) {
        Response<Object> response = new Response<Object>();
        response.setStatusCode(HttpStatus.NOT_FOUND.value());
        response.setMessage(ex.getMessage());
        response.setError("404 Not Found.URL không tồn tại");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Object>> validationError(
            MethodArgumentNotValidException ex) {

        BindingResult result = ex.getBindingResult();
        final List<FieldError> fieldErrors = result.getFieldErrors();

        Response<Object> response = new Response<Object>();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setError(ex.getBody().getDetail());
        List<String> errors = fieldErrors.stream().map(f -> f.getDefaultMessage()).collect(Collectors.toList());
        response.setMessage(errors.size() > 1 ? errors : errors.get(0));

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


}
