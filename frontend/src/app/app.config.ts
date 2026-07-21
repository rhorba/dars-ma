import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { firstValueFrom, catchError, of } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { I18nService } from './core/i18n/i18n.service';
import { AuthService } from './core/auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    // provideTranslateService registers its own default (no-op) TranslateLoader provider;
    // since Angular DI resolves multiple same-token providers to the last one registered,
    // the HTTP loader must come after it or it gets silently shadowed and no translations load.
    provideTranslateService({ fallbackLang: 'fr' }),
    provideTranslateHttpLoader({ prefix: '/i18n/', suffix: '.json' }),
    provideAppInitializer(() => {
      inject(I18nService).init();
      // The access token lives in memory only (never persisted) - on every full page load
      // (hard refresh, deep link, bookmark) the app must attempt a silent restore via the
      // httpOnly refresh cookie before the router evaluates any guard, or a valid session
      // gets treated as logged-out. Swallow failure: guests with no valid cookie just stay
      // logged out, which is the correct behavior for them.
      const authService = inject(AuthService);
      return firstValueFrom(authService.refresh().pipe(catchError(() => of(null))));
    })
  ]
};
