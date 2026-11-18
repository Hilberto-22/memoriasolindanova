import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api, Collection } from '../../../api';
import { resource } from '../../../data-state';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { EditorialImagePicker } from '../editorial-image-picker/editorial-image-picker.component';

@Component({
  imports:[ReactiveFormsModule, ErrorState, LoadingState, EditorialImagePicker],
  templateUrl:'./collection-editor.component.html'
})
export class CollectionEditor {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.collections());
  readonly editing = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    slug:['',[Validators.required,Validators.pattern('[a-z0-9]+(?:-[a-z0-9]+)*')]],
    title:['',Validators.required], description:[''], coverMemory:[null as number | null]
  });
  select(item?:Collection) {
    this.editing.set(Boolean(item));
    this.form.reset({
      slug:item?.slug ?? '', title:item?.title ?? '', description:item?.description ?? '',
      coverMemory:item?.coverMemory ?? null
    });
    this.error.set('');
  }
  save() {
    if (this.form.invalid || this.busy()) {this.form.markAllAsTouched(); return;}
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api.saveCollection(this.form.getRawValue(),this.editing()).subscribe({
      next:() => {this.busy.set(false); this.message.set('Coleção salva.'); this.select(); this.data.refresh();},
      error:(failure:HttpErrorResponse) => {this.busy.set(false); this.error.set(failure.error?.message ?? 'Não foi possível salvar a coleção.');}
    });
  }
}
