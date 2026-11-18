package br.org.memorias.application;

import br.org.memorias.persistence.ArchiveRepository;
import org.springframework.stereotype.Component;

@Component
class EditorialImageSelection {
    private final ArchiveRepository archive;

    EditorialImageSelection(ArchiveRepository archive) {
        this.archive = archive;
    }

    Long validate(Long id) {
        if (id == null) return null;

        var memory = archive.detail(id);
        if (memory == null || memory.imageUrl() == null) {
            throw InputChecks.bad("Selecione um registro publicado com imagem.");
        }
        return id;
    }
}
