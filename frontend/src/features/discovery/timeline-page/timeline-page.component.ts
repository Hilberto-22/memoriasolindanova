import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Memory } from '../../../api';
import { resource } from '../../../data-state';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { PageTitle } from '../../../shared/components/page-title/page-title.component';
import { TimelineItem } from '../timeline-item/timeline-item.component';

@Component({
  imports:[RouterLink, PageTitle, TimelineItem, LoadingState, EmptyState, ErrorState],
  templateUrl: './timeline-page.component.html'
})
export class TimelinePage {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.timeline());
  readonly decade = signal('');
  readonly direction = signal('oldest');
  private decadeOf(item: Memory) { return Math.floor((item.year ?? 0)/10)*10; }
  readonly decades = computed(() => [...new Set((this.data.state().data ?? []).map(item => this.decadeOf(item)))].sort((a,b) => a-b));
  readonly items = computed(() => (this.data.state().data ?? [])
    .filter(item => !this.decade() || this.decadeOf(item) === Number(this.decade()))
    .sort((a,b) => (this.direction() === 'newest' ? -1 : 1) * ((a.year ?? 0) - (b.year ?? 0)) || a.id - b.id));
}
