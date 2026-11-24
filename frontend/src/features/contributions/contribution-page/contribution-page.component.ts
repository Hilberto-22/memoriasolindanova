import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { of } from 'rxjs';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { PageTitle } from '../../../shared/components/page-title/page-title.component';
import { FileUploader } from '../file-uploader/file-uploader.component';
import { materialTypes } from '../../../shared/material-types';

@Component({
  imports:[ReactiveFormsModule, RouterLink, PageTitle, FileUploader, ErrorState],
  templateUrl: './contribution-page.component.html'
})
export class ContributionPage {
  private readonly api = inject(Api);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly route = inject(ActivatedRoute);
  readonly related = Number(this.route.snapshot.queryParamMap.get('registro')) || null;
  readonly admin = this.route.snapshot.data['admin'] === true;
  readonly types = materialTypes.map(item => item.type);
  readonly form = this.fb.group({
    name:['', this.admin ? [] : Validators.required],
    email:['', this.admin ? [Validators.email] : [Validators.required,Validators.email]], type:['',Validators.required],
    title:['',Validators.required], description:['', this.admin ? [] : Validators.required], period:[''], place:[''],
    collection:['',Validators.required], source:['', this.admin ? [] : Validators.required],
    consent:[false, this.admin ? [] : Validators.requiredTrue]
  });
  readonly collections = resource(() => this.api.collections());
  readonly options = resource(() => this.admin ? this.api.recordOptions() : of({places:[], sources:[]}));
  readonly requiredFields = this.admin
    ? ['type','collection','title'] as const
    : ['name','email','type','title','description','collection','source','consent'] as const;
  readonly files = signal<File[]>([]);
  readonly sending = signal(false);
  readonly progress = signal<number | null>(null);
  readonly error = signal('');
  readonly receipt = signal<number | null>(null);
  completed() {
    return this.requiredFields
      .filter(field => this.form.controls[field].valid).length;
  }
  submit() {
    if (this.sending()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set(this.admin
        ? 'Preencha Tipo de material, Coleção e Título. Se informar um e-mail, confira se ele é válido.'
        : 'Preencha os campos obrigatórios, informe um e-mail válido e marque a autorização.');
      return;
    }
    this.error.set('');
    this.progress.set(null);
    this.sending.set(true);
    const payload = new FormData();
    payload.append('memory', new Blob([JSON.stringify({...this.form.getRawValue(), relatedMemory:this.related})], {type:'application/json'}));
    for (const file of this.files()) payload.append('files', file, file.name);
    this.api.contribute(payload,this.admin).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress && event.total) this.progress.set(Math.round(100 * event.loaded / event.total));
        if (event.type === HttpEventType.Response && event.body) {
          this.receipt.set(event.body.id);
          this.sending.set(false);
        }
      },
      error: (failure: HttpErrorResponse) => {
        this.sending.set(false);
        this.error.set(failure.status === 413
          ? 'O servidor recusou o tamanho do envio. Vídeos MP4 podem ter até 300 MB cada; os demais arquivos, até 20 MB. Se os arquivos estão dentro desses limites, avise a equipe do acervo.'
          : failure.error?.message ?? 'Não foi possível enviar a contribuição. Tente novamente.');
      }
    });
  }
}
