import { signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Observable, catchError, map, of, startWith, switchMap } from 'rxjs';

export type Result<T> = { phase: 'loading' | 'error'; data?: undefined } | { phase: 'ready'; data: T };

export function stateOf<T>(source: Observable<T>): Observable<Result<T>> {
  return source.pipe(
    map(data => ({phase:'ready', data} as const)),
    catchError(() => of({phase:'error'} as const)),
    startWith({phase:'loading'} as const)
  );
}

export function resource<T>(load: () => Observable<T>) {
  const revision = signal(0);
  const state = toSignal(toObservable(revision).pipe(switchMap(() => stateOf(load()))), {
    initialValue:{phase:'loading'} as Result<T>
  });
  return {state, refresh: () => revision.update(value => value + 1)};
}
