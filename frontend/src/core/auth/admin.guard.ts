import { inject } from "@angular/core";
import { CanActivateChildFn, Router } from "@angular/router";
import { catchError, map, of } from "rxjs";
import { Api } from "../../api";

export const requireAdmin: CanActivateChildFn = (_route, state) => {
  const router = inject(Router);
  return inject(Api)
    .session()
    .pipe(
      map(() => true),
      catchError(() => of(router.createUrlTree(["/entrar"], {
        queryParams: { returnUrl: state.url }
      }))),
    );
};
