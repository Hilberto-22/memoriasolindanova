import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  return next(request).pipe(catchError((failure: HttpErrorResponse) => {
    if (failure.status === 401 && request.url.startsWith('/api/admin/') &&
        /^\/admin(?:\/|\?|#|$)/.test(router.url)) {
      void router.navigate(['/entrar'], {
        queryParams: { returnUrl: router.url, reason: 'expired' }
      });
    }
    return throwError(() => failure);
  }));
};
