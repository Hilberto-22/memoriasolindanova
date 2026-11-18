package br.org.memorias.web;

import br.org.memorias.application.CollectionEditor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/collections")
class CollectionEditorController {
    private final CollectionEditor editor;
    CollectionEditorController(CollectionEditor editor) { this.editor = editor; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void create(@RequestBody CollectionEditor.Edit input) { editor.create(input); }

    @PutMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void edit(@PathVariable String slug, @RequestBody CollectionEditor.Edit input) { editor.edit(slug, input); }
}
