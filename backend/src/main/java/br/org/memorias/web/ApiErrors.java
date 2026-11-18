package br.org.memorias.web;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> requestError(ResponseStatusException failure) {
        return ResponseEntity.status(failure.getStatusCode()).body(Map.of("message",
                Optional.ofNullable(failure.getReason()).orElse("Recurso indisponível.")));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> uploadError() {
        return ResponseEntity.status(413).body(Map.of("message", "O tamanho permitido para o envio foi excedido."));
    }
}
