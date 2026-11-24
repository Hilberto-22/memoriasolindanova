import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { combineLatest, map, switchMap } from 'rxjs';
import { Api } from '../../../api';
import { resource, stateOf } from '../../../data-state';
import { ArchiveCard } from '../../../shared/components/archive-card/archive-card.component';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { Pagination } from '../../../shared/components/pagination/pagination.component';
import { PortalIcon } from '../../../shared/components/portal-icon/portal-icon.component';
import { SearchBar } from '../../../shared/components/search-bar/search-bar.component';
import { materialTypes } from '../../../shared/material-types';
import { emptyFacets, Filters, filtersOf } from '../catalog-filters';
import { FilterPanel } from '../filter-panel/filter-panel.component';

@Component({
  imports:[RouterLink, SearchBar, FilterPanel, LoadingState, ErrorState, EmptyState, ArchiveCard, Pagination, PortalIcon],
  templateUrl:'./catalog-page.component.html'
})
export class CatalogPage {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly collections = resource(() => this.api.collections());
  readonly facets = resource(() => this.api.facets());
  readonly counts = resource(() => this.api.materialCounts());
  readonly editorial = resource(() => this.api.portalContent());
  readonly emptyFacets = emptyFacets;
  readonly reload = signal(0);
  readonly list = signal(false);
  readonly increment = (value: number) => value + 1;
  readonly cards = computed(() => {
    const counts = new Map(this.counts.state().data?.map(item => [item.label,item.count]));
    return materialTypes.map(item => ({...item,count:counts.get(item.type) ?? 0}));
  });
  private readonly params = toSignal(this.route.queryParamMap, {requireSync:true});
  readonly filters = computed(() => filtersOf(this.params()));
  readonly query = computed(() => ({
    ...this.filters(), q:this.params().get('q') ?? '', sort:this.params().get('sort') ?? 'recent',
    page:Math.max(0, Number.parseInt(this.params().get('page') ?? '0',10) || 0)
  }));
  readonly result = toSignal(combineLatest([toObservable(this.query), toObservable(this.reload)]).pipe(
    switchMap(([query]) => stateOf(this.api.catalog({...query,size:10})))));
  pages(total:number, size:number) { return Math.ceil(total / size); }
  applyFilters(filters:Filters) { this.navigate({...filters,page:0}); }
  navigate(updates:Record<string,string|number>) {
    const queryParams = Object.fromEntries(Object.entries({...this.query(),...updates}).filter(([,value]) => value !== '' && value !== 0));
    void this.router.navigate([], {relativeTo:this.route,queryParams});
  }
}
