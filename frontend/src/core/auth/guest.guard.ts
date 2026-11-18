import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { Api } from '../../api';
import { adminDestination } from './admin-destination';

export const requireGuest: CanActivateFn = (route) => {
  const router = inject(Router);
  return inject(Api).session().pipe(
    map(() => router.parseUrl(adminDestination(route.queryParamMap.get('returnUrl')))),
    catchError(() => of(true))
  );
};
