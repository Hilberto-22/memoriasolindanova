import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { of } from 'rxjs';
import { Api, Contribution } from '../../../api';
import { resource } from '../../../data-state';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge.component';
import { FileGallery } from '../../../shared/components/file-gallery/file-gallery.component';

@Component({
  imports:[RouterLink, ReactiveFormsModule, ConfirmDialog, EmptyState, ErrorState, LoadingState, StatusBadge, FileGallery],
  templateUrl: './review-page.component.html'
})
export class ReviewPage {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.contributions());
  readonly selected = signal<number | null>(Number(inject(ActivatedRoute).snapshot.queryParamMap.get('registro')) || null);
  readonly error = signal('');
  readonly success = signal('');
  readonly busy = signal(false);
  readonly form = inject(FormBuilder).group({
    category:['',Validators.required], credits:['',Validators.required], usage:['',Validators.required],
    year:[null as number | null], latitude:[null as number | null], longitude:[null as number | null]
  });
  select(id: number, open: boolean) {
    if (open && this.selected() !== id) {this.selected.set(id); this.form.reset(); this.error.set('');}
    else if (!open && this.selected() === id) this.selected.set(null);
  }
  review(item: Contribution, status: 'APPROVED' | 'REJECTED') {
    if (this.busy()) return;
    if (status === 'APPROVED' && this.form.invalid) {
      this.error.set('Preencha categoria, créditos e condições de uso e confira as datas e coordenadas.');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api.review(item.id, {status, ...(status === 'APPROVED' ? this.form.getRawValue() : {})}).subscribe({
      next: () => {
        this.busy.set(false);
        this.success.set(status === 'APPROVED' ? 'Registro publicado.' : 'Contribuição recusada.');
        this.selected.set(null);
        this.data.refresh();
      },
      error: (failure: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(failure.error?.message ?? 'Não foi possível concluir a revisão.');
      }
    });
  }
}
