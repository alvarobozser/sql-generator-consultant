import { Injectable, signal, computed, effect, Signal } from '@angular/core';

import { Language, TranslationKey } from './i18n.types';
import { TRANSLATIONS } from './translations';

const STORAGE_KEY = 'sqlgen.language';

/**
 * Servicio de internacionalizacion (i18n).
 *
 * <p>Mantiene el idioma actual como signal (Angular 19). Persiste en
 * localStorage. Provee `t(key)` para obtener la traduccion.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  /** Signal privado (writable) del idioma actual. */
  private readonly _language = signal<Language>(this.loadInitial());

  /** Signal publico de solo lectura. */
  readonly language: Signal<Language> = this._language.asReadonly();

  /** Signal computado: el diccionario del idioma actual. */
  readonly translations = computed(() => TRANSLATIONS[this._language()]);

  constructor() {
    // Cada vez que cambia el idioma, lo persistimos en localStorage.
    effect(() => {
      try {
        localStorage.setItem(STORAGE_KEY, this._language());
      } catch {
        // Si localStorage no esta disponible (SSR, etc.), ignorar.
      }
    });
  }

  /** Cambia el idioma. */
  setLanguage(lang: Language): void {
    this._language.set(lang);
  }

  /** Toggle entre 'es' y 'en'. */
  toggleLanguage(): void {
    this._language.update(current => (current === 'es' ? 'en' : 'es'));
  }

  /** Devuelve la traduccion de la clave en el idioma actual. */
  t(key: TranslationKey): string {
    return this.translations()[key];
  }

  /** Carga el idioma inicial desde localStorage, o 'es' por defecto. */
  private loadInitial(): Language {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'es' || stored === 'en') {
        return stored;
      }
    } catch {
      // Ignorar errores de localStorage.
    }
    return 'es';
  }
}
