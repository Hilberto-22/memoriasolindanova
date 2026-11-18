package br.org.memorias.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.memorias.application.PortalContentService;
import br.org.memorias.domain.PortalContent;

@RestController
@RequestMapping("/api")
public class PortalContentController {
    private final PortalContentService content;
    public PortalContentController(PortalContentService content) { this.content = content; }

    @GetMapping("/portal-content")
    public PortalContent.View read() { return content.read(); }

    @PutMapping("/admin/portal-content")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@RequestBody PortalContent input) { content.save(input); }
}
