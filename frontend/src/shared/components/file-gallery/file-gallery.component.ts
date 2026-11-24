import { Component, input, signal } from '@angular/core';
import { MemoryFile } from '../../../api';

@Component({
  selector: 'file-gallery',
  templateUrl: './file-gallery.component.html',
  styleUrl: './file-gallery.component.scss',
})
export class FileGallery {
  readonly files = input.required<MemoryFile[]>();
  readonly failed = signal<number[]>([]);

  mediaError(id: number) {
    this.failed.update(ids => ids.includes(id) ? ids : [...ids, id]);
  }

  downloadUrl(file: MemoryFile) {
    return file.url + (file.url.includes('?') ? '&' : '?') + 'download=true';
  }
}
