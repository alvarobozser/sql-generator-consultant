import { TranslationDict } from './i18n.types';

/**
 * Traducciones ES/EN de todos los textos de la UI.
 * Centralizadas aqui para facilitar cambios.
 */
export const TRANSLATIONS: TranslationDict = {
  es: {
    title: 'AI SQL Query Generator',
    subtitle: 'Convierte preguntas en lenguaje natural a SQL con Claude',
    question_placeholder: 'p.ej. ¿Cuántos usuarios hay de España?',
    submit: 'Generar SQL y ejecutar',
    thinking: 'Pensando...',
    generated_sql: 'SQL generado',
    results: 'Resultados',
    rows_count: 'filas',
    no_results: 'La consulta no devolvió filas.',
    error: 'Error',
    language_toggle: 'Idioma',
    db_status: 'Estado de la base de datos',
    db_check: 'Verificar conexión',
    db_url: 'URL de la BD',
    db_connected: 'Conectado',
    db_disconnected: 'No conectado',
    examples_title: 'Ejemplos para probar',
    example_1: '¿Cuántos usuarios hay en total?',
    example_2: "Dame los 5 productos más caros de 'electronics'",
    example_3: '¿Cuánto han gastado en total los usuarios de España?',
  },
  en: {
    title: 'AI SQL Query Generator',
    subtitle: 'Convert natural language questions to SQL with Claude',
    question_placeholder: 'e.g. How many users are from Spain?',
    submit: 'Generate SQL and execute',
    thinking: 'Thinking...',
    generated_sql: 'Generated SQL',
    results: 'Results',
    rows_count: 'rows',
    no_results: 'The query returned no rows.',
    error: 'Error',
    language_toggle: 'Language',
    db_status: 'Database status',
    db_check: 'Check connection',
    db_url: 'Database URL',
    db_connected: 'Connected',
    db_disconnected: 'Disconnected',
    examples_title: 'Examples to try',
    example_1: 'How many users are there in total?',
    example_2: "Show me the 5 most expensive products in 'electronics'",
    example_3: 'How much have users from Spain spent in total?',
  },
};
