import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { App } from './app.component';
import { routes } from './app.routes';
import { sessionInterceptor } from './core/auth/session.interceptor';

bootstrapApplication(App, {
  providers:[provideHttpClient(withInterceptors([sessionInterceptor])), provideRouter(routes, withInMemoryScrolling({
    anchorScrolling:'enabled', scrollPositionRestoration:'top'
  }))]
}).catch(error => console.error(error));
