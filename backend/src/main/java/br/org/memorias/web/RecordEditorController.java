package br.org.memorias.web;

import br.org.memorias.application.RecordEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/admin/memories")
class RecordEditorController {
    private final RecordEditor editor;
    RecordEditorController(RecordEditor editor) { this.editor = editor; }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void edit(@PathVariable long id, @RequestBody RecordEditor.Edit edit) { editor.edit(id, edit); }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void editWithFiles(@PathVariable long id, @RequestPart("memory") RecordEditor.Edit edit,
                       @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        editor.edit(id, edit, files == null ? List.of() : files);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) { editor.delete(id); }
}
