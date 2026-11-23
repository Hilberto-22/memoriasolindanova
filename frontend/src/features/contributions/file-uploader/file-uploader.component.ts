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
    const invalid = files.length > 5 || files.some(file => {
      const video = file.type === 'video/mp4' || /\.mp4$/i.test(file.name);
      return !file.size || file.size > (video ? 300 : 20) * 1024 * 1024;
    });
    this.error.set(invalid ? 'Selecione até cinco arquivos. Vídeos MP4: até 300 MB cada; demais arquivos: até 20 MB cada.' : '');
    if (invalid) input.value = '';
    this.names.set(invalid ? [] : files.map(file => file.name));
    this.changed.emit(invalid ? [] : files);
  }
}
