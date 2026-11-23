package br.org.memorias.application;

import br.org.memorias.domain.ArchiveModels.RecordOptions;
import br.org.memorias.domain.RecordOption;
import br.org.memorias.persistence.RecordOptionRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static br.org.memorias.application.InputChecks.*;

@Service
public class RecordOptionService {
    private final RecordOptionRepository options;

    public RecordOptionService(RecordOptionRepository options) { this.options = options; }

    public List<RecordOption> list() { return options.list(); }

    public RecordOptions suggestions() {
        var entries = list();
        return new RecordOptions(entries.stream().filter(item -> item.kind().equals("PLACE")).map(RecordOption::value).toList(),
                entries.stream().filter(item -> item.kind().equals("SOURCE")).map(RecordOption::value).toList());
    }

    public long create(RecordOption.Input input) {
        var data = validate(input);
        try {
            return options.create(data);
        } catch (DuplicateKeyException failure) {
            throw duplicate();
        }
    }

    public void update(long id, RecordOption.Input input) {
        var data = validate(input);
        try {
            if (!options.update(id, data)) throw missing();
        } catch (DuplicateKeyException failure) {
            throw duplicate();
        }
    }

    public void delete(long id) {
        if (!options.delete(id)) throw missing();
    }

    private RecordOption.Input validate(RecordOption.Input input) {
        String kind = required(input.kind(), 6, "Lista");
        if (!kind.equals("PLACE") && !kind.equals("SOURCE")) throw bad("Selecione Local ou Origem do material.");
        String value = required(input.value(), kind.equals("PLACE") ? 180 : 500,
                kind.equals("PLACE") ? "Local" : "Origem do material");
        return new RecordOption.Input(kind, value);
    }

    private static ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Esta opção já está cadastrada nesta lista.");
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Opção não encontrada.");
    }
}
