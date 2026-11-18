import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { of, switchMap } from 'rxjs';
import { Api } from '../../../api';
import { stateOf } from '../../../data-state';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { Pagination } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'editorial-image-picker',
  imports: [LoadingState, Pagination],
  templateUrl: './editorial-image-picker.component.html'
})
export class EditorialImagePicker {
  private readonly api = inject(Api);
  readonly label = input.required<string>();
  readonly selected = input<number | null>(null);
  readonly disabled = input(false);
  readonly imagesOnly = input(true);
  readonly selectedChange = output<number | null>();
  readonly expanded = signal(false);
  readonly search = signal({ q: '', page: 0 });
  private readonly filters = computed(() => this.expanded() ? this.search() : null);
  readonly results = toSignal(toObservable(this.filters).pipe(switchMap(filters => filters
    ? stateOf(this.api.catalog({ ...filters, size: 6, withImage: this.imagesOnly() ? 'true' : '' }))
    : of(null))));
  readonly current = toSignal(toObservable(this.selected).pipe(switchMap(id => id === null
    ? of(null)
    : stateOf(this.api.memory(String(id))))));

  lookup(q: string, page = 0) {
    this.search.set({ q: q.trim(), page });
  }

  pages(total: number, size: number) {
    return Math.ceil(total / size);
  }

  choose(id: number) {
    this.selectedChange.emit(id);
    this.expanded.set(false);
  }
}
