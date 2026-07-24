import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { QueryService } from '../../core/api/query.service';
import { QueryResponse } from '../../core/api/query.types';
import { I18nService } from '../../core/i18n/i18n.service';
import { Language } from '../../core/i18n/i18n.types';

/**
 * Componente principal: hace preguntas en lenguaje natural al backend
 * Spring Boot y muestra SQL + resultados en una tabla.
 */
@Component({
  selector: 'app-query',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './query.component.html',
  styleUrls: ['./query.component.css']
})
export class QueryComponent {
  private readonly queryService = inject(QueryService);
  readonly i18n = inject(I18nService);

  /** Estado reactivo con signals. */
  readonly question = signal<string>('');
  readonly loading = signal<boolean>(false);
  readonly result = signal<QueryResponse | null>(null);

  /** Computed: hay resultados para mostrar? */
  readonly hasResult = computed(() => this.result() !== null);

  /** Computed: hay error? */
  readonly hasError = computed(() => {
    const r = this.result();
    return r !== null && r.error !== null;
  });

  /** Computed: hay SQL generado (incluso si fallo)? */
  readonly hasSql = computed(() => {
    const r = this.result();
    return r !== null && r.sql !== null && r.sql !== '';
  });

  /** Computed: hay filas de resultados? */
  readonly hasRows = computed(() => {
    const r = this.result();
    return r !== null && r.rows !== null && r.rows.length > 0;
  });

  /** Computed: columnas de la tabla de resultados. */
  readonly columns = computed(() => {
    const r = this.result();
    return r?.columns ?? [];
  });

  /** Computed: filas de la tabla. */
  readonly rows = computed(() => {
    const r = this.result();
    return r?.rows ?? [];
  });

  /** Computed: el idioma actual (para binding en template). */
  readonly language = computed<Language>(() => this.i18n.language());

  /** Maneja el submit del formulario. */
  async onSubmit(): Promise<void> {
    const q = this.question().trim();
    if (!q || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.result.set(null);

    try {
      const response = await firstValueFrom(
        this.queryService.ask({
          question: q,
          language: this.i18n.language(),
        })
      );
      this.result.set(response);
    } catch (err: any) {
      // Error HTTP (4xx/5xx) o de red.
      this.result.set({
        sql: '',
        rows: [],
        columns: [],
        error: err?.error?.error || err?.message || 'Error desconocido',
      });
    } finally {
      this.loading.set(false);
    }
  }

  /** Cambia el idioma y limpia resultados (la pregunta puede no tener sentido en el otro idioma). */
  onLanguageToggle(): void {
    this.i18n.toggleLanguage();
  }

  /** Click en un ejemplo: lo pone en el textarea. */
  onExampleClick(example: string): void {
    this.question.set(example);
  }
}
