package br.org.memorias.web;

import br.org.memorias.application.RecordEditor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/memories")
class RecordEditorController {
    private final RecordEditor editor;
    RecordEditorController(RecordEditor editor) { this.editor = editor; }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void edit(@PathVariable long id, @RequestBody RecordEditor.Edit edit) { editor.edit(id, edit); }
}
