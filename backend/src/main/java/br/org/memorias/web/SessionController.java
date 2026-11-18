package br.org.memorias.web;

import java.security.Principal;
import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api")
class SessionController {

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }

    @GetMapping("/session")
    public Map<String, String> session(Principal principal) { return Map.of("name", principal.getName()); }
}
