package br.org.memorias.web;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.org.memorias.domain.ArchiveModels.Collection;
import br.org.memorias.domain.ArchiveModels.Facets;
import br.org.memorias.domain.ArchiveModels.Memory;
import br.org.memorias.domain.ArchiveModels.Page;
import br.org.memorias.persistence.ArchiveRepository;
import br.org.memorias.persistence.CollectionRepository;

@RestController
@RequestMapping("/api")
public class ArchiveController {
    private final ArchiveRepository archive;
    private final CollectionRepository collections;

    public ArchiveController(ArchiveRepository archive, CollectionRepository collections) {
        this.archive = archive;
        this.collections = collections;
    }

    @GetMapping("/collections")
    public List<Collection> collections() {
        return collections.collections();
    }

    @GetMapping("/facets")
    public Facets facets() {
        return archive.facets();
    }

    @GetMapping("/memories")
    public Page catalog(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String collection,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String year,
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to,
            @RequestParam(defaultValue = "") String place,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "false") boolean withImage) {
        if ((from != null && (from < 1 || from > 2200)) || (to != null && (to < 1 || to > 2200))
                || (from != null && to != null && from > to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido.");
        }
        return archive.catalog(q, collection, category, type, year, from, to, place, sort, page, size, withImage);
    }

    @GetMapping("/memories/{id}")
    public Memory detail(@PathVariable long id) {
        return Optional.ofNullable(archive.detail(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/map")
    public List<Memory> map() {
        return archive.map();
    }

    @GetMapping("/timeline")
    public List<Memory> timeline() {
        return archive.timeline();
    }
}
