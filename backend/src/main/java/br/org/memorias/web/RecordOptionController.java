package br.org.memorias.web;

import br.org.memorias.application.RecordOptionService;
import br.org.memorias.domain.ArchiveModels.RecordOptions;
import br.org.memorias.domain.RecordOption;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/record-options")
public class RecordOptionController {
    private final RecordOptionService options;

    public RecordOptionController(RecordOptionService options) { this.options = options; }

    @GetMapping
    public RecordOptions suggestions() { return options.suggestions(); }

    @GetMapping("/entries")
    public List<RecordOption> list() { return options.list(); }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@RequestBody RecordOption.Input input) { return Map.of("id", options.create(input)); }

    @PutMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable long id, @RequestBody RecordOption.Input input) { options.update(id, input); }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { options.delete(id); }
}
