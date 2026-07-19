/** Idiomas soportados por la app. */
export type Language = 'es' | 'en';

/** Diccionario de traducciones: clave -> { es, en }. */
export type TranslationKey =
  | 'title'
  | 'subtitle'
  | 'question_placeholder'
  | 'submit'
  | 'thinking'
  | 'generated_sql'
  | 'results'
  | 'rows_count'
  | 'no_results'
  | 'error'
  | 'language_toggle'
  | 'db_status'
  | 'db_check'
  | 'db_url'
  | 'db_connected'
  | 'db_disconnected'
  | 'examples_title'
  | 'example_1'
  | 'example_2'
  | 'example_3';

export type Translations = Record<TranslationKey, string>;
export type TranslationDict = Record<Language, Translations>;
