import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type AppLang = 'fr' | 'ar' | 'en';

const RTL_LANGS: AppLang[] = ['ar'];
const STORAGE_KEY = 'dars_ma_lang';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly translate = inject(TranslateService);
  readonly currentLang = signal<AppLang>('fr');

  init(): void {
    const stored = (localStorage.getItem(STORAGE_KEY) as AppLang | null) ?? 'fr';
    this.use(stored);
  }

  use(lang: AppLang): void {
    this.translate.use(lang);
    this.currentLang.set(lang);
    localStorage.setItem(STORAGE_KEY, lang);
    document.documentElement.lang = lang;
    document.documentElement.dir = RTL_LANGS.includes(lang) ? 'rtl' : 'ltr';
  }
}
