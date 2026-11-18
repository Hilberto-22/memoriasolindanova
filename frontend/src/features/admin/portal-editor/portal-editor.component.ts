import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, PortalContent } from '../../../api';
import { resource } from '../../../data-state';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { EditorialImagePicker } from '../editorial-image-picker/editorial-image-picker.component';

@Component({
  imports:[ReactiveFormsModule, RouterLink, ErrorState, LoadingState, EditorialImagePicker],
  templateUrl:'./portal-editor.component.html'
})
export class PortalEditor {
  private readonly api = inject(Api);
  readonly content = resource(() => this.api.portalContent());
  readonly busy = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    heroTitle:['',Validators.required], heroDescription:['',Validators.required],
    communityTitle:['',Validators.required], communityDescription:['',Validators.required],
    about:[''], guardians:[''], contactEmail:['',Validators.email],
    featuredMemory:[null as number|null]
  });
  constructor() {
    effect(() => {
      const content = this.content.state().data?.content;
      if (content) this.form.reset(content);
    });
  }
  save() {
    if (this.form.invalid || this.busy()) {this.form.markAllAsTouched(); return;}
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    const payload:PortalContent = this.form.getRawValue();
    this.api.savePortalContent(payload).subscribe({
      next:() => {this.busy.set(false); this.message.set('Conteúdo do portal atualizado.');},
      error:(failure:HttpErrorResponse) => {this.busy.set(false); this.error.set(failure.error?.message ?? 'Não foi possível salvar o conteúdo.');}
    });
  }
}
