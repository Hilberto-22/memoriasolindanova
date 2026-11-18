import { Component, output, signal } from '@angular/core';

@Component({
  selector:'file-uploader',
  templateUrl: './file-uploader.component.html'
})
export class FileUploader {
  readonly changed = output<File[]>();
  readonly error = signal('');
  readonly names = signal<string[]>([]);
  select(input: HTMLInputElement) {
    const files = Array.from(input.files ?? []);
    const invalid = files.length > 5 || files.some(file => !file.size || file.size > 20 * 1024 * 1024);
    this.error.set(invalid ? 'Selecione até cinco arquivos com até 20 MB cada.' : '');
    if (invalid) input.value = '';
    this.names.set(invalid ? [] : files.map(file => file.name));
    this.changed.emit(invalid ? [] : files);
  }
}
