import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, CityHistoryEntry } from '../../../api';
import { resource } from '../../../data-state';
import { cityHistoryTheme, cityHistoryThemes, historyPeriod } from '../../../shared/city-history';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { EditorialImagePicker } from '../editorial-image-picker/editorial-image-picker.component';

function dateRange(control: AbstractControl) {
  const { startDate, endDate } = control.value;
  return endDate && (!startDate || endDate < startDate) ? { dateRange: true } : null;
}

@Component({
  imports: [ReactiveFormsModule, RouterLink, ConfirmDialog, EmptyState, ErrorState, LoadingState, EditorialImagePicker],
  templateUrl: './city-history-editor.component.html',
})
export class CityHistoryEditor {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.cityHistory());
  readonly themes = cityHistoryThemes;
  readonly themeLabel = cityHistoryTheme;
  readonly periodLabel = historyPeriod;
  readonly editing = signal<number | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(180), Validators.pattern(/\S/)]],
    category: ['history', Validators.required],
    content: ['', [Validators.required, Validators.maxLength(20000), Validators.pattern(/\S/)]],
    period: ['', Validators.maxLength(120)],
    startDate: [''], endDate: [''], source: ['', Validators.maxLength(1000)], memoryId: [null as number | null],
  }, { validators: dateRange });

  select(item?: CityHistoryEntry) {
    if (this.busy()) return;
    this.editing.set(item?.id ?? null);
    this.form.reset({ title: item?.title ?? '', category: item?.category ?? 'history', content: item?.content ?? '',
      period: item?.period ?? '', startDate: item?.startDate ?? '', endDate: item?.endDate ?? '',
      source: item?.source ?? '', memoryId: item?.memoryId ?? null });
    this.error.set('');
    this.message.set('');
  }

  save() {
    if (this.busy()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api.saveCityHistory({ ...value, startDate: value.startDate || null, endDate: value.endDate || null }, this.editing()).subscribe({
      next: () => {
        this.busy.set(false); this.select(); this.message.set('Informação salva e disponível na página pública.'); this.data.refresh();
      },
      error: (failure: HttpErrorResponse) => this.failed(failure, 'Não foi possível salvar a informação.'),
    });
  }

  remove(id: number) {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api.deleteCityHistory(id).subscribe({
      next: () => {
        this.busy.set(false);
        if (this.editing() === id) this.select();
        this.message.set('Informação removida.'); this.data.refresh();
      },
      error: (failure: HttpErrorResponse) => this.failed(failure, 'Não foi possível remover a informação.'),
    });
  }

  private failed(failure: HttpErrorResponse, fallback: string) {
    this.busy.set(false); this.error.set(failure.error?.message ?? fallback);
  }
}
