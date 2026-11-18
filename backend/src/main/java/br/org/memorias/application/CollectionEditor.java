package br.org.memorias.application;

import br.org.memorias.persistence.CollectionRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static br.org.memorias.application.InputChecks.*;

@Service
public class CollectionEditor {
    private final CollectionRepository collections;
    private final EditorialImageSelection images;

    CollectionEditor(CollectionRepository collections, EditorialImageSelection images) {
        this.collections = collections;
        this.images = images;
    }

    public void create(Edit input) {
        String slug = required(input.slug(),50,"Identificador");
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) throw bad("Use letras minúsculas, números e hífens no identificador.");
        try {
            collections.create(slug, required(input.title(),150,"Título"),
                    optional(input.description(),400,"Descrição"), images.validate(input.coverMemory()));
        } catch (DuplicateKeyException conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Já existe uma coleção com esse identificador.");
        }
    }

    public void edit(String slug, Edit input) {
        if (!collections.update(slug, required(input.title(),150,"Título"),
                optional(input.description(),400,"Descrição"), images.validate(input.coverMemory()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public record Edit(String slug, String title, String description, Long coverMemory) {}
}
