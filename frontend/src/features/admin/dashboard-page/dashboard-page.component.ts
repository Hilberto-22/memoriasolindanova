import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { DashboardMetricCard } from '../../../shared/components/dashboard-metric-card/dashboard-metric-card.component';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { PortalIcon } from '../../../shared/components/portal-icon/portal-icon.component';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge.component';
import { DashboardDistribution } from '../dashboard-distribution/dashboard-distribution.component';
import { DashboardTrend } from '../dashboard-trend/dashboard-trend.component';

@Component({
  imports:[DatePipe, RouterLink, DashboardMetricCard, EmptyState, ErrorState, LoadingState, PortalIcon, StatusBadge, DashboardDistribution, DashboardTrend],
  templateUrl:'./dashboard-page.component.html'
})
export class DashboardPage {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.dashboard());
  readonly editorial = resource(() => this.api.portalContent());
  readonly types = computed(() => new Map(this.data.state().data?.byType.map(item => [item.label,item.count])));
  readonly statuses = computed(() => (this.data.state().data?.byStatus ?? []).map(item => ({
    ...item,label:({APPROVED:'Aprovadas',PENDING:'Em análise',REJECTED:'Recusadas'} as Record<string,string>)[item.label] ?? item.label
  })));
}
