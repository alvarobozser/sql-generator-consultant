package com.sqlgen.infrastructure.config;

/**
 * Configuracion de la integracion con Anthropic Claude.
 *
 * <p>Usamos HTTP directo (RestClient) en lugar de Spring AI por un bug conocido
 * de Spring AI 1.x con Anthropic (envia Authorization: Bearer en vez de x-api-key).
 *
 * <p>El bean AnthropicLlmAdapter se crea con @Component, asi que este archivo
 * queda solo como documentacion. En el futuro, si Spring AI arregla el bug,
 * podemos volver a usarlo.
 */
// Esta clase esta vacia intencionalmente. La integracion con Anthropic se hace
// directamente en AnthropicLlmAdapter usando RestClient.
// Se mantiene el archivo como punto de extension futuro.
