import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { I18nService } from './i18n.service';

describe('I18nService', () => {
  let service: I18nService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateService({ fallbackLang: 'fr' })]
    });
    service = TestBed.inject(I18nService);
  });

  it('defaults document direction to ltr for French', () => {
    service.use('fr');
    expect(document.documentElement.dir).toBe('ltr');
    expect(service.currentLang()).toBe('fr');
  });

  it('switches document direction to rtl for Arabic', () => {
    service.use('ar');
    expect(document.documentElement.dir).toBe('rtl');
    expect(document.documentElement.lang).toBe('ar');
  });

  it('persists the chosen language to localStorage', () => {
    service.use('en');
    expect(localStorage.getItem('dars_ma_lang')).toBe('en');
  });

  it('init() restores the previously stored language', () => {
    localStorage.setItem('dars_ma_lang', 'ar');
    service.init();
    expect(service.currentLang()).toBe('ar');
    expect(document.documentElement.dir).toBe('rtl');
  });
});
