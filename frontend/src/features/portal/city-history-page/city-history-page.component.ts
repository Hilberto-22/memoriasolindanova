import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { cityHistoryTheme, cityHistoryThemes, groupHistory, historyPeriod, orderHistory } from '../../../shared/city-history';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { PageTitle } from '../../../shared/components/page-title/page-title.component';

@Component({
  imports: [DatePipe, RouterLink, PageTitle, EmptyState, ErrorState, LoadingState],
  templateUrl: './city-history-page.component.html',
  styleUrl: './city-history-page.component.scss',
})
export class CityHistoryPage {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.cityHistory());
  readonly themes = cityHistoryThemes;
  readonly themeLabel = cityHistoryTheme;
  readonly periodLabel = historyPeriod;
  readonly theme = signal('');
  readonly period = signal('');
  readonly direction = signal('oldest');
  readonly grouping = signal('date');
  readonly periods = computed(() => groupHistory(this.data.state().data ?? [], 'oldest').map(group => group.label));
  readonly items = computed(() => orderHistory((this.data.state().data ?? []).filter(item =>
    (!this.theme() || item.category === this.theme()) && (!this.period() || historyPeriod(item) === this.period())), this.direction()));
  readonly groups = computed(() => this.grouping() === 'period'
    ? groupHistory(this.items(), this.direction())
    : [{ label: '', items: this.items() }]);

  reset() { this.theme.set(''); this.period.set(''); this.direction.set('oldest'); this.grouping.set('date'); }
}
