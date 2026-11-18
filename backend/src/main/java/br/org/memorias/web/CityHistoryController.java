package br.org.memorias.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import br.org.memorias.application.CityHistoryService;
import br.org.memorias.domain.CityHistory;

@RestController
@RequestMapping("/api")
public class CityHistoryController {
    private final CityHistoryService history;

    public CityHistoryController(CityHistoryService history) { this.history = history; }

    @GetMapping("/city-history")
    public List<CityHistory.Entry> list() { return history.list(); }

    @PostMapping("/admin/city-history")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@RequestBody CityHistory.Input input) {
        return Map.of("id", history.create(input));
    }

    @PutMapping("/admin/city-history/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable long id, @RequestBody CityHistory.Input input) { history.update(id, input); }

    @DeleteMapping("/admin/city-history/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { history.delete(id); }
}
