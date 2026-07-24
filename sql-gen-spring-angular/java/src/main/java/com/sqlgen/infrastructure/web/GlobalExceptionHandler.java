package com.sqlgen.infrastructure.web;

import com.sqlgen.domain.exception.EmptyQuestionException;
import com.sqlgen.domain.exception.LlmException;
import com.sqlgen.domain.exception.SqlExecutionException;
import com.sqlgen.domain.exception.UnsafeSqlException;
import com.sqlgen.infrastructure.web.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para los controllers REST.
 * Convierte excepciones de dominio a respuestas HTTP con codigos de estado
 * apropiados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmptyQuestionException.class)
    public ResponseEntity<ErrorResponse> handleEmptyQuestion(EmptyQuestionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(UnsafeSqlException.class)
    public ResponseEntity<ErrorResponse> handleUnsafeSql(UnsafeSqlException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(SqlExecutionException.class)
    public ResponseEntity<ErrorResponse> handleSqlExecution(SqlExecutionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ErrorResponse> handleLlm(LlmException e) {
        log.error("Error del LLM", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.of("Error con el proveedor de IA: " + e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Errores de validacion", java.util.Map.of("fields", details)));
    }
}
