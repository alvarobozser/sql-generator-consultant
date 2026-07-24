/** Tipos compartidos por los servicios y el componente Query. */

/** Peticion al endpoint POST /api/v1/queries. */
export interface QueryRequest {
  question: string;
  language: 'es' | 'en';
}

/** Respuesta del endpoint POST /api/v1/queries. */
export interface QueryResponse {
  sql: string;
  rows: Array<Record<string, unknown>>;
  columns: string[];
  error: string | null;
}
