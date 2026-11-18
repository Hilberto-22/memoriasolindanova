package br.org.memorias.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.memorias.domain.CityHistory;
import br.org.memorias.persistence.ArchiveRepository;
import br.org.memorias.persistence.CityHistoryRepository;

import static br.org.memorias.application.InputChecks.*;

@Service
public class CityHistoryService {
    private final CityHistoryRepository history;
    private final ArchiveRepository archive;
    private static final Set<String> CATEGORIES = Set.of("history", "traditions", "flag", "anthem", "other");

    public CityHistoryService(CityHistoryRepository history, ArchiveRepository archive) {
        this.history = history;
        this.archive = archive;
    }

    public List<CityHistory.Entry> list() { return history.list(); }

    public long create(CityHistory.Input input) { return history.create(validate(input)); }

    public void update(long id, CityHistory.Input input) {
        if (!history.update(id, validate(input))) throw missing();
    }

    public void delete(long id) {
        if (!history.delete(id)) throw missing();
    }

    private CityHistory.Input validate(CityHistory.Input input) {
        String category = required(input.category(), 30, "Tema");
        if (!CATEGORIES.contains(category)) throw bad("Selecione um tema válido.");
        checkDate(input.startDate());
        checkDate(input.endDate());
        if (input.endDate() != null && (input.startDate() == null || input.endDate().isBefore(input.startDate())))
            throw bad("Informe uma data inicial e uma data final igual ou posterior a ela.");
        if (input.memoryId() != null && !archive.published(input.memoryId()))
            throw bad("Selecione um registro publicado do acervo.");
        return new CityHistory.Input(required(input.title(), 180, "Título"), category,
                required(input.content(), 20000, "Conteúdo"), optional(input.period(), 120, "Período"),
                input.startDate(), input.endDate(), optional(input.source(), 1000, "Fontes"), input.memoryId());
    }

    private static void checkDate(LocalDate date) {
        if (date != null && (date.getYear() < 1 || date.getYear() > 9999))
            throw bad("As datas devem ter um ano entre 1 e 9999.");
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Informação da cidade não encontrada.");
    }
}
