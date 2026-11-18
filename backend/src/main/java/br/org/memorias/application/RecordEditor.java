package br.org.memorias.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static br.org.memorias.application.InputChecks.bad;
import static br.org.memorias.application.InputChecks.optional;
import static br.org.memorias.application.InputChecks.required;
import br.org.memorias.domain.PublicationMetadata;
import br.org.memorias.persistence.CollectionRepository;
import br.org.memorias.persistence.PublicationRepository;

@Service
public class RecordEditor {
    private final PublicationRepository records;
    private final CollectionRepository collections;

    public RecordEditor(PublicationRepository records, CollectionRepository collections) {
        this.records = records;
        this.collections = collections;
    }

    public void edit(long id, Edit edit) {
        String collection = required(edit.collectionSlug(), 50, "Coleção");
        if (!collections.exists(collection))
            throw bad("Coleção inválida.");
        var data = new PublicationMetadata(edit.category(), edit.credits(), edit.usage(), edit.year(), edit.latitude(),
                edit.longitude());
        boolean changed = records.update(id, required(edit.title(), 180, "Título"), data.category(), collection,
                required(edit.description(), 6000, "Descrição"), optional(edit.period(), 150, "Período"),
                optional(edit.place(), 180, "Local"), required(edit.source(), 500, "Fonte"),
                data.credits(), data.usage(), data.year(), data.latitude(), data.longitude());
        if (!changed)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public record Edit(String title, String category, String collectionSlug, String description, String period,
            String place,
            String source, String credits, String usage, Integer year, Double latitude, Double longitude) {
    }
}
