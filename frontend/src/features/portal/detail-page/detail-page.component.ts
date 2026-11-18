import { Component, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { combineLatest, switchMap } from 'rxjs';
import { Api, Memory } from '../../../api';
import { stateOf } from '../../../data-state';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { PageTitle } from '../../../shared/components/page-title/page-title.component';

@Component({
  imports:[RouterLink, PageTitle, ErrorState, LoadingState],
  templateUrl: './detail-page.component.html'
})
export class DetailPage {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  readonly reload = signal(0);
  readonly increment = (value: number) => value + 1;
  readonly shareState = signal('');
  readonly result = toSignal(combineLatest([this.route.paramMap, toObservable(this.reload)]).pipe(
    switchMap(([params]) => stateOf(this.api.memory(params.get('id') ?? '')))));
  async share(item: Memory) {
    try {
      if (navigator.share) await navigator.share({title:item.title, url:location.href});
      else { await navigator.clipboard.writeText(location.href); this.shareState.set('Link copiado.'); }
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) this.shareState.set('Copie o endereço desta página para compartilhar.');
    }
  }
}
