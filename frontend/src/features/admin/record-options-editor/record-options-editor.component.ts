import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api, RecordOption } from '../../../api';
import { resource } from '../../../data-state';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';

function optionLength(control: AbstractControl) {
  const { kind, value } = control.value;
  return value?.length > (kind === 'PLACE' ? 180 : 500) ? { optionLength: true } : null;
}

@Component({
  imports: [ReactiveFormsModule, ConfirmDialog, EmptyState, ErrorState, LoadingState],
  templateUrl: './record-options-editor.component.html',
  styles: ['.option-name { min-width: 0; overflow-wrap: anywhere; }'],
})
export class RecordOptionsEditor {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.recordOptionEntries());
  readonly groups = [{kind:'PLACE', label:'Locais'}, {kind:'SOURCE', label:'Origens do material'}] as const;
  readonly editing = signal<number | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    kind: ['PLACE' as 'PLACE' | 'SOURCE', Validators.required],
    value: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(500)]],
  }, {validators: optionLength});

  entries(kind: 'PLACE' | 'SOURCE') {
    return this.data.state().data?.filter(item => item.kind === kind) ?? [];
  }

  select(item?: RecordOption) {
    if (this.busy()) return;
    this.editing.set(item?.id ?? null);
    this.form.reset({kind:item?.kind ?? this.form.controls.kind.value, value:item?.value ?? ''});
    this.error.set('');
    this.message.set('');
  }

  save() {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api.saveRecordOption(this.form.getRawValue(), this.editing()).subscribe({
      next: () => {
        this.busy.set(false); this.select(); this.message.set('Opção salva e disponível nos formulários de registros.'); this.data.refresh();
      },
      error: (failure: HttpErrorResponse) => this.failed(failure, 'Não foi possível salvar a opção.'),
    });
  }

  remove(id: number) {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api.deleteRecordOption(id).subscribe({
      next: () => {
        this.busy.set(false); this.select(); this.message.set('Opção removida da lista.'); this.data.refresh();
      },
      error: (failure: HttpErrorResponse) => this.failed(failure, 'Não foi possível remover a opção.'),
    });
  }

  private failed(failure: HttpErrorResponse, fallback: string) {
    this.busy.set(false); this.error.set(failure.error?.message ?? fallback);
  }
}
