import { Component, computed, inject, signal } from '@angular/core';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { ArchiveCard } from '../../../shared/components/archive-card/archive-card.component';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { PageTitle } from '../../../shared/components/page-title/page-title.component';
import { MemoryMap } from '../memory-map/memory-map.component';

@Component({
  imports:[PageTitle, MemoryMap, ArchiveCard, LoadingState, EmptyState, ErrorState],
  templateUrl: './map-page.component.html'
})
export class MapPage {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.map());
  readonly place = signal('');
  readonly places = computed(() => [...new Set(this.data.state().data?.map(item => item.place).filter(Boolean))].sort());
  readonly items = computed(() => (this.data.state().data ?? []).filter(item => !this.place() || item.place === this.place()));
}
