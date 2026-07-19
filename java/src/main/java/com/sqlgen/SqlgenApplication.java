package com.sqlgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Entry point del backend Spring Boot.
 *
 * <p>Antes de arrancar Spring, carga variables del archivo .env (compartido
 * con la version Python) en System properties. Asi Spring puede leer
 * ANTHROPIC_API_KEY, DATABASE_URL, etc. via ${...}.
 */
@SpringBootApplication
public class SqlgenApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(SqlgenApplication.class, args);
    }

    /**
     * Busca el archivo .env en varias ubicaciones y carga sus variables
     * como System properties (si no estan ya definidas como env var).
     */
    private static void loadDotenv() {
        String[] paths = { ".env", "../.env", "../../.env" };
        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    for (String line : Files.readAllLines(file.toPath())) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = trimmed.indexOf('=');
                        if (eqIdx > 0) {
                            String key = trimmed.substring(0, eqIdx).trim();
                            String value = trimmed.substring(eqIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (System.getenv(key) == null && System.getProperty(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[WARN] No se pudo leer .env: " + e.getMessage());
                }
                return;
            }
        }
    }
}
