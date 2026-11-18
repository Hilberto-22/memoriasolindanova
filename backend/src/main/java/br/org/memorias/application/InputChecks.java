package br.org.memorias.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class InputChecks {
    private InputChecks() {}
    public static String required(String value, int max, String field) {
        String result = optional(value, max, field);
        if (result.isEmpty()) throw bad(field + " é obrigatório.");
        return result;
    }

    public static String optional(String value, int max, String field) {
        String result = value == null ? "" : value.strip();
        if (result.length() > max) throw bad(field + " deve ter até " + max + " caracteres.");
        return result;
    }

    public static ResponseStatusException bad(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
