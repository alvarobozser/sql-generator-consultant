import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { QueryRequest, QueryResponse } from './query.types';

/**
 * Servicio HTTP para hablar con el backend Spring Boot.
 *
 * <p>Usa el proxy de Angular dev server para evitar CORS en desarrollo:
 *   /api/* -> http://localhost:8080/api/*
 */
@Injectable({ providedIn: 'root' })
export class QueryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  /** Envia una pregunta al backend y devuelve la respuesta (sql + rows + error). */
  ask(request: QueryRequest): Observable<QueryResponse> {
    return this.http.post<QueryResponse>(`${this.baseUrl}/queries`, request);
  }

  /** Health check (debug). */
  health(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${this.baseUrl}/health`);
  }
}
