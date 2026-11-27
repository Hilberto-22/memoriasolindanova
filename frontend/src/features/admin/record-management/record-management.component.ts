import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, switchMap } from 'rxjs';
import { Api } from '../../../api';
import { stateOf } from '../../../data-state';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { Pagination } from '../../../shared/components/pagination/pagination.component';
import { SearchBar } from '../../../shared/components/search-bar/search-bar.component';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  imports:[RouterLink, EmptyState, LoadingState, Pagination, SearchBar, ConfirmDialog],
  templateUrl:'./record-management.component.html'
})
export class RecordManagement {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly revision = new BehaviorSubject(0);
  readonly removing = signal<number | null>(null);
  readonly error = signal('');
  readonly message = signal('');
  readonly result = toSignal(combineLatest([this.route.queryParamMap, this.revision]).pipe(switchMap(([params]) => stateOf(this.api.catalog({
    q:params.get('q') ?? '', page:Number.parseInt(params.get('page') ?? '0',10) || 0
  })))));
  readonly query = toSignal(this.route.queryParamMap,{requireSync:true});
  search(q:string, page=0) {
    void this.router.navigate([], {relativeTo:this.route,queryParams:{q:q || null,page:page || null}});
  }
  pages(total:number,size:number) { return Math.ceil(total/size); }
  remove(id:number) {
    if (this.removing() !== null) return;
    this.removing.set(id);
    this.error.set('');
    this.message.set('');
    this.api.deleteMemory(id).subscribe({
      next:() => {
        this.removing.set(null);
        this.message.set('Registro removido do acervo.');
        this.revision.next(this.revision.value + 1);
      },
      error:(failure:HttpErrorResponse) => {
        this.removing.set(null);
        this.error.set(failure.error?.message ?? 'Não foi possível remover o registro.');
      }
    });
  }
}
