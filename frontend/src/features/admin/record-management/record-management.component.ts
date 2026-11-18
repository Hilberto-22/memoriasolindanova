import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { Api } from '../../../api';
import { stateOf } from '../../../data-state';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { Pagination } from '../../../shared/components/pagination/pagination.component';
import { SearchBar } from '../../../shared/components/search-bar/search-bar.component';

@Component({
  imports:[RouterLink, EmptyState, LoadingState, Pagination, SearchBar],
  templateUrl:'./record-management.component.html'
})
export class RecordManagement {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly result = toSignal(this.route.queryParamMap.pipe(switchMap(params => stateOf(this.api.catalog({
    q:params.get('q') ?? '', page:Number.parseInt(params.get('page') ?? '0',10) || 0
  })))));
  readonly query = toSignal(this.route.queryParamMap,{requireSync:true});
  search(q:string, page=0) {
    void this.router.navigate([], {relativeTo:this.route,queryParams:{q:q || null,page:page || null}});
  }
  pages(total:number,size:number) { return Math.ceil(total/size); }
}
