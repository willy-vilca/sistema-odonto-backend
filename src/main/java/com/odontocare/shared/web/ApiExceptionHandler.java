package com.odontocare.shared.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.transaction.CannotCreateTransactionException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({DataAccessException.class, CannotCreateTransactionException.class})
    public ProblemDetail handleDatabaseUnavailable(Exception exception) {
        log.warn("Database operation failed: {}", exception.getClass().getSimpleName());
        return ApiProblems.create(HttpStatus.SERVICE_UNAVAILABLE, "Servicio temporalmente no disponible",
                "No pudimos acceder a la información. Intenta nuevamente en unos momentos.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        var detail = ApiProblems.create(HttpStatus.BAD_REQUEST, "Datos inválidos", "Revisa los campos indicados.");
        List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage())).toList();
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected request failure: {}", exception.getClass().getSimpleName());
        return ApiProblems.create(HttpStatus.INTERNAL_SERVER_ERROR, "No pudimos completar la solicitud",
                "Intenta nuevamente. Si continúa, comunica el código de referencia al soporte.");
    }

    private record FieldErrorResponse(String field, String message) { }
}
