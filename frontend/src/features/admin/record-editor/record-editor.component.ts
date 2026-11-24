import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { publicationCategories, PUBLICATION_USAGE_SUGGESTIONS } from '../publication-options';

@Component({
  imports:[ReactiveFormsModule, RouterLink, ErrorState, LoadingState],
  templateUrl:'./record-editor.component.html'
})
export class RecordEditor {
  private readonly api = inject(Api);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly record = resource(() => this.api.memory(String(this.id)));
  readonly collections = resource(() => this.api.collections());
  readonly options = resource(() => this.api.recordOptions());
  readonly facets = resource(() => this.api.facets());
  readonly categories = computed(() => publicationCategories(this.facets.state().data?.categories));
  readonly usageSuggestions = PUBLICATION_USAGE_SUGGESTIONS;
  readonly busy = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    title:['',Validators.required], category:['',Validators.required], collectionSlug:['',Validators.required],
    description:['',Validators.required], period:[''], place:[''], source:['',Validators.required],
    credits:['',Validators.required], usage:['',Validators.required],
    year:[null as number|null], latitude:[null as number|null], longitude:[null as number|null]
  });
  constructor() {
    effect(() => {
      const item = this.record.state().data;
      if (item) this.form.reset(item);
    });
  }
  save() {
    if (this.form.invalid || this.busy()) {this.form.markAllAsTouched(); return;}
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api.editMemory(this.id,this.form.getRawValue()).subscribe({
      next:() => {this.busy.set(false); this.message.set('Registro atualizado no acervo público.');},
      error:(failure:HttpErrorResponse) => {this.busy.set(false); this.error.set(failure.error?.message ?? 'Não foi possível salvar o registro.');}
    });
  }
}
