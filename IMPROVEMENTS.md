# Plan de Mejoras del Harness

> Documento generado a partir del análisis crítico del harness tras completar dos
> features end-to-end (Issue #1: Python/Streamlit, Issue #2: Java/Spring Boot/Angular).
>
> **Audiencia**: developers que quieran tomar este harness como base y mejorarlo
> para sus propios proyectos. Las mejoras están organizadas por prioridad y
> nivel de esfuerzo estimado.

---

## Tabla de contenidos

1. [Resumen ejecutivo](#1-resumen-ejecutivo)
2. [Mejoras de corto plazo (1-2 horas)](#2-mejoras-de-corto-plazo-1-2-horas)
3. [Mejoras de medio plazo (1-2 días)](#3-mejoras-de-medio-plazo-1-2-días)
4. [Mejoras de largo plazo (refactor del harness)](#4-mejoras-de-largo-plazo-refactor-del-harness)
5. [Cambios de diseño del harness](#5-cambios-de-diseño-del-harness)
6. [Skills que faltan](#6-skills-que-faltan)
7. [Configuración del orquestador mejorada](#7-configuración-del-orquestador-mejorada)
8. [Plantillas de documentos mejoradas](#8-plantillas-de-documentos-mejoradas)
9. [Checklist pre-merge](#9-checklist-pre-merge)
10. [Lecciones aprendidas](#10-lecciones-aprendidas)

---

## 1. Resumen ejecutivo

### Deficiencias identificadas

| # | Deficiencia | Severidad | Esfuerzo | Impacto |
|---|---|---|---|---|
| 1 | Sin CI en GitHub Actions | Alta | 1h | Bugs/security issues pasan sin detección |
| 2 | Sin Dependabot / security scanning | Alta | 30m | CVEs no detectados |
| 3 | Sin linter/formatter en Java | Alta | 2h | Código Java inconsistente |
| 4 | Sin cobertura de tests medida | Media | 1h | No sabes qué % está cubierto |
| 5 | El orquestador es solo docs | Media | 1 día | El flujo no se ejecuta automáticamente |
| 6 | Context Manager no se activa | Media | 1 día | Calidad cae en sesiones largas sin aviso |
| 7 | Skills son markdown, no ejecutables | Media | 2 días | No se amortiza la inversión |
| 8 | Plan estático, se desvía sin avisar | Alta | 1 día | Cambios se pierden o contradicen |
| 9 | Memoria mezclada y sin schema | Media | 1 día | Difícil de debuggear/extender |
| 10 | Sin Reviewer real | Alta | 2 días | Calidad depende del acierto del agente |
| 11 | Sin autonomía del orquestador | Media | 1 día | Requiere babysitting constante |
| 12 | Sin aprendizaje postmortem | Media | 30m por feature | Errores se repiten |

### Roadmap sugerido

```
Corto plazo (1-2h)  →  Resuelve #1, #2, #4, #9
                         "Endurecimiento" del proyecto para portfolio.

Medio plazo (1-2d)  →  Resuelve #3, #5, #6, #7
                         "Automatización parcial" del flujo del agente.

Largo plazo (1 sem)  →  Resuelve #8, #10, #11, #12
                         "Harness de verdad" con autonomía y aprendizaje.
```

---

## 2. Mejoras de corto plazo (1-2 horas)

### 2.1. CI en GitHub Actions

**Problema**: nada verifica automáticamente que los tests pasan, el código compila y el linter está limpio antes de mergear.

**Solución**: crear `.github/workflows/ci.yml`.

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  # ============================================================
  # PYTHON
  # ============================================================
  python:
    name: Python (sql-gen-python)
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: sql-gen-python
    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
          cache: 'pip'

      - name: Install dependencies
        run: |
          python -m pip install --upgrade pip
          pip install -r requirements.txt
          pip install pytest pytest-cov ruff pip-audit

      - name: Lint
        run: ruff check .

      - name: Tests
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
        run: pytest -q --cov=. --cov-report=xml

      - name: Security audit
        run: pip-audit

      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with:
          file: sql-gen-python/coverage.xml
          flags: python

  # ============================================================
  # JAVA + ANGULAR (en paralelo)
  # ============================================================
  java:
    name: Java (sql-gen-spring-angular/java)
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: sql-gen-spring-angular/java
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_USER: sqlgen
          POSTGRES_PASSWORD: sqlgen_dev_password
          POSTGRES_DB: sqlgen
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Run tests
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/sqlgen
          SPRING_DATASOURCE_USERNAME: sqlgen
          SPRING_DATASOURCE_PASSWORD: sqlgen_dev_password
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
        run: mvn verify

  angular:
    name: Angular (sql-gen-spring-angular/frontend)
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: sql-gen-spring-angular/frontend
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install
        run: npm ci

      - name: Build
        run: npm run build

      - name: Lint
        run: npm run lint
```

**Beneficio**: cada push verifica automáticamente que todo compila y pasa. Bloquea merges rotos.

---

### 2.2. Dependabot para alertas de CVEs

**Problema**: las dependencias pueden tener CVEs publicados y no nos enteramos.

**Solución**: crear `.github/dependabot.yml`.

```yaml
# .github/dependabot.yml
version: 2
updates:
  # Python (sql-gen-python)
  - package-ecosystem: "pip"
    directory: "/sql-gen-python"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    labels:
      - "dependencies"
      - "python"
    commit-message:
      prefix: "deps(python)"

  # Java (sql-gen-spring-angular/java)
  - package-ecosystem: "maven"
    directory: "/sql-gen-spring-angular/java"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    labels:
      - "dependencies"
      - "java"
    commit-message:
      prefix: "deps(java)"

  # Angular (sql-gen-spring-angular/frontend)
  - package-ecosystem: "npm"
    directory: "/sql-gen-spring-angular/frontend"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    labels:
      - "dependencies"
      - "angular"
    commit-message:
      prefix: "deps(angular)"

  # GitHub Actions
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
```

**Beneficio**: PRs automáticos cada semana con updates de seguridad. Configurar también CodeQL en `.github/workflows/codeql.yml`:

```yaml
# .github/workflows/codeql.yml
name: "CodeQL"
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 6 * * 1'  # semanal los lunes
jobs:
  analyze:
    runs-on: ubuntu-latest
    permissions:
      security-events: write
    steps:
      - uses: actions/checkout@v4
      - uses: github/codeql-action/init@v3
        with:
          languages: python, javascript, java
      - uses: github/codeql-action/analyze@v3
```

**Beneficio**: análisis estático de seguridad automático (SQL injection, XSS, path traversal, etc.) en Python, JS y Java.

---

### 2.3. Cobertura de tests con JaCoCo (Java)

**Problema**: no medimos qué % de código está cubierto por tests. Para Java no hay nada configurado.

**Solución**: añadir JaCoCo al `pom.xml`.

```xml
<!-- sql-gen-spring-angular/java/pom.xml, en la sección <build><plugins> -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Ver cobertura: `mvn test` → genera `target/site/jacoco/index.html`.

Para CI, añadir a `.github/workflows/ci.yml`:
```yaml
- name: Code coverage
  run: mvn jacoco:report
- name: Upload to Codecov
  uses: codecov/codecov-action@v4
  with:
    file: sql-gen-spring-angular/java/target/site/jacoco/jacoco.xml
    flags: java
```

**Beneficio**: ves exactamente qué clases/métodos no están testeados. Codecov te da badges y tracking histórico.

---

### 2.4. Pre-commit hooks (local, no CI)

**Problema**: el "lint" solo se ejecuta si te acuerdas. Falta enforcement local.

**Solución**: instalar `pre-commit` y crear `.pre-commit-config.yaml`.

```bash
pip install pre-commit
pre-commit install
```

```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/astral-sh/ruff-pre-commit
    rev: v0.6.9
    hooks:
      - id: ruff
        args: [--fix, --exit-non-zero-on-fix]
        files: '^sql-gen-python/.*\.py$'

  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v5.0.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-json
      - id: detect-private-key
        # CRITICO: detecta claves privadas (RSA, SSH, etc.) antes de commit
      - id: check-merge-conflict
      - id: check-added-large-files
        args: [--maxkb=500]

  # Hook custom: buscar secrets en archivos staged
  - repo: local
    hooks:
      - id: no-secrets
        name: No secrets in staged files
        entry: python -c "
import re, subprocess, sys
patterns = [
    r'sk-ant-(?:api\\d{2}-)?[A-Za-z0-9_-]{20,}',
    r'sk-S8fpyF5dX[a-zA-Z0-9]{15,}',
    r'sk-(?:proj-)?[A-Za-z0-9_-]{20,}',
    r'AIza[0-9A-Za-z_-]{35}',
    r'gh[oprsu]_[A-Za-z0-9_]{36,}',
    r'AKIA[0-9A-Z]{16}',
    r'-----BEGIN (?:RSA |EC )?PRIVATE KEY-----',
]
result = subprocess.run(['git', 'diff', '--cached'], capture_output=True, text=True)
diff = result.stdout
issues = []
for p in patterns:
    for m in re.finditer(p, diff):
        line = diff[diff.rfind(chr(10), 0, m.start())+1 : diff.find(chr(10), m.end())].strip()
        if 'your_key' in line.lower() or 'placeholder' in line.lower():
            continue
        issues.append(line[:80])
if issues:
    print('SECRETS DETECTADOS:')
    for i in issues:
        print(f'  {i}')
    sys.exit(1)
print('OK: no secrets in staged files')
"
        language: system
        pass_filenames: false
        stages: [pre-commit]
```

**Beneficio**: bloquea el commit si hay secrets, llaves privadas, JSON/YAML inválido, etc. **Antes** de que llegue a GitHub.

---

## 3. Mejoras de medio plazo (1-2 días)

### 3.1. Linter y formatter para Java (Spotless + Checkstyle)

**Problema**: el código Java no tiene formatter automático. Cada developer escribe distinto.

**Solución**: añadir Spotless al `pom.xml`.

```xml
<!-- sql-gen-spring-angular/java/pom.xml -->
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat/>
        </java>
    </configuration>
</plugin>
```

Uso:
```bash
mvn spotless:apply    # formatea todo el codigo
mvn spotless:check    # solo verifica (en CI)
```

Añadir al CI:
```yaml
- name: Spotless check
  run: mvn spotless:check
```

**Beneficio**: formato consistente, sin discusiones en PRs sobre estilo.

---

### 3.2. Orquestador ejecutable (script bash/Python)

**Problema**: el orquestador es un `.harness/agents.md` con instrucciones, pero nadie lo ejecuta. El usuario (humano) hace de orquestador.

**Solución**: crear un script `harness.sh` (o `harness.py`) que implemente el flujo.

```bash
# harness.sh - Orquestador ejecutable del harness SDD
#!/usr/bin/env bash
set -euo pipefail

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Paths
HARNESS_DIR=".harness"
MEMORY_FILE="$HARNESS_DIR/memory/current-progress.json"
ISSUE_NUMBER=$(jq -r '.github_issue_number // empty' "$MEMORY_FILE" 2>/dev/null || echo "")

# Funciones
status() { echo -e "${BLUE}== $1 ==${NC}"; }
warn()   { echo -e "${YELLOW}WARN: $1${NC}"; }
ok()     { echo -e "${GREEN}OK: $1${NC}"; }
fail()   { echo -e "${RED}FAIL: $1${NC}"; exit 1; }

# Comando principal
case "${1:-help}" in
    status)
        # Mostrar estado actual
        status "Estado del harness"
        if [ ! -f "$MEMORY_FILE" ]; then
            echo "  No hay feature en curso."
            exit 0
        fi
        CURRENT=$(jq -r '.current_task // "N/A"' "$MEMORY_FILE")
        STATUS=$(jq -r '.status // "N/A"' "$MEMORY_FILE")
        echo "  Status: $STATUS"
        echo "  Current task: $CURRENT"
        if [ -n "$ISSUE_NUMBER" ]; then
            echo "  GitHub Issue: #$ISSUE_NUMBER"
        fi
        ;;

    next)
        # Sugerir siguiente paso
        status "Siguiente paso sugerido"
        STATUS=$(jq -r '.status // "done"' "$MEMORY_FILE")
        case "$STATUS" in
            "done")
                echo "  Feature completa. Puedes empezar una nueva con: harness new <nombre>"
                ;;
            "awaiting_research_approval")
                echo "  Esperando aprobacion del research-plan.md. Revisa y aprueba, luego: harness approve"
                ;;
            "awaiting_plan_approval")
                echo "  Esperando aprobacion del tech-plan.md. Revisa y aprueba, luego: harness approve"
                ;;
            "in_progress")
                NEXT=$(jq -r '.next_task // "Revisar tasks pendientes"' "$MEMORY_FILE")
                echo "  Feature en progreso. Siguiente: $NEXT"
                ;;
            "blocked")
                REASON=$(jq -r '.blocked_reason // "N/A"' "$MEMORY_FILE")
                echo "  Feature bloqueada: $REASON"
                ;;
        esac
        ;;

    approve)
        # Aprobar la fase actual y avanzar
        PHASE="${2:-}"
        case "$PHASE" in
            research)
                jq '.status = "awaiting_plan_approval"' "$MEMORY_FILE" > "$MEMORY_FILE.tmp"
                mv "$MEMORY_FILE.tmp" "$MEMORY_FILE"
                ok "Research aprobado. Siguiente fase: planning."
                ;;
            plan)
                jq '.status = "in_progress"' "$MEMORY_FILE" > "$MEMORY_FILE.tmp"
                mv "$MEMORY_FILE.tmp" "$MEMORY_FILE"
                ok "Plan aprobado. Implementacion puede comenzar."
                ;;
            *)
                fail "Especifica la fase: harness approve [research|plan]"
                ;;
        esac
        ;;

    audit)
        # Ejecutar auditorias de seguridad
        status "Auditoria de seguridad"
        if [ -f "audit_security.py" ]; then
            python audit_security.py
        else
            warn "audit_security.py no encontrado"
        fi
        ;;

    test)
        # Correr todos los tests
        status "Ejecutando todos los tests"
        # Python
        if [ -d "sql-gen-python" ]; then
            echo "Python:"
            (cd sql-gen-python && pytest -q) || fail "Python tests fallaron"
        fi
        # Java
        if [ -d "sql-gen-spring-angular/java" ]; then
            echo "Java:"
            (cd sql-gen-spring-angular/java && mvn test -q) || fail "Java tests fallaron"
        fi
        # Angular
        if [ -d "sql-gen-spring-angular/frontend" ]; then
            echo "Angular:"
            (cd sql-gen-spring-angular/frontend && npm run build) || fail "Angular build fallo"
        fi
        ok "Todos los tests/builds pasaron"
        ;;

    help|*)
        echo "Uso: $0 {status|next|approve|audit|test}"
        echo ""
        echo "  status  - Muestra el estado actual del harness"
        echo "  next    - Sugiere el siguiente paso"
        echo "  approve - Aprueba una fase (research o plan)"
        echo "  audit   - Ejecuta auditoria de seguridad"
        echo "  test    - Corre todos los tests del proyecto"
        ;;
esac
```

Uso:
```bash
./harness.sh status       # que estado tiene el proyecto
./harness.sh next         # que hago ahora
./harness.sh audit        # hay secretos
./harness.sh test         # pasan los tests
./harness.sh approve plan # apruebo el plan
```

**Beneficio**: el orquestador deja de ser "documento" y se convierte en **herramienta ejecutable**. El agente y el humano usan el mismo flujo.

---

### 3.3. Context Manager que se auto-activa

**Problema**: el contexto crece durante sesiones largas, baja la calidad, y no se detecta.

**Solución**: hook que mide el contexto y dispara una consolidación.

```python
# .harness/scripts/context_manager.py
"""
Context Manager: detecta senales de degradacion y dispara consolidacion.
Senales:
  - Muchos archivos releidos (solapamiento)
  - Mensajes largos repetitivos
  - Tokens consumidos altos
"""
import json
import os
import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
MEMORY_FILE = REPO_ROOT / ".harness/memory/context-stats.json"

def measure_context() -> dict:
    """Mide el estado del contexto y devuelve metricas."""
    stats = {
        "files_read_count": 0,        # cuantos archivos se han leido
        "files_read_unique": set(),    # archivos unicos
        "messages_count": 0,          # total de mensajes
        "avg_message_length": 0,      # longitud promedio
        "duplicated_content": 0,      # contenido repetido literal
    }

    # Leer el transcript (si esta disponible).
    transcript_path = REPO_ROOT / ".harness/memory/transcript.md"
    if not transcript_path.exists():
        return stats

    content = transcript_path.read_text(encoding="utf-8")
    # Contar archivos leidos (heuristica: "Read tool" en el log).
    files = re.findall(r"Read tool.*?([\w/]+\.\w+)", content)
    stats["files_read_count"] = len(files)
    stats["files_read_unique"] = set(files)

    # Contar mensajes y longitud.
    messages = content.split("\n\n---\n\n")
    stats["messages_count"] = len(messages)
    if messages:
        stats["avg_message_length"] = sum(len(m) for m in messages) // len(messages)

    # Detectar contenido duplicado (bloques de > 100 chars que aparecen > 3 veces).
    blocks = re.findall(r"```[\s\S]{100,}?```", content)
    if blocks:
        from collections import Counter
        counter = Counter(blocks)
        stats["duplicated_content"] = sum(1 for b, count in counter.items() if count > 3)

    return stats

def check_signals(stats: dict) -> list[str]:
    """Devuelve una lista de senales de degradacion detectadas."""
    signals = []

    # Solapamiento: mismo archivo leido 3+ veces.
    repeated = [f for f, count in Counter(stats["files_read_unique"]).items() if count >= 3]
    if repeated:
        signals.append(f"SOLAPAMIENTO: {len(repeated)} archivos leidos 3+ veces: {repeated[:3]}")

    # Mensajes muy largos = contexto cargado.
    if stats["avg_message_length"] > 5000:
        signals.append(f"CONTEXTO LARGO: mensajes promedio {stats['avg_message_length']} chars")

    # Contenido duplicado = respuestas redundantes.
    if stats["duplicated_content"] > 5:
        signals.append(f"REDUNDANCIA: {stats['duplicated_content']} bloques repetidos")

    return signals

def main():
    stats = measure_context()
    signals = check_signals(stats)

    print("=" * 60)
    print(" Context Manager")
    print("=" * 60)
    print(f"  Archivos leidos: {stats['files_read_count']} ({len(stats['files_read_unique']} unicos)")
    print(f"  Mensajes: {stats['messages_count']} (promedio {stats['avg_message_length']} chars)")

    if signals:
        print("\n  SENALES DE DEGRADACION DETECTADAS:")
        for s in signals:
            print(f"    {s}")
        print("\n  Recomendacion: consolidar contexto, generar resumen, empezar sesion nueva")
    else:
        print("\n  [OK] Contexto saludable")

    # Guardar metricas para el orchestrator.
    MEMORY_FILE.parent.mkdir(parents=True, exist_ok=True)
    stats_serializable = {**stats, "files_read_unique": list(stats["files_read_unique"])}
    MEMORY_FILE.write_text(json.dumps(stats_serializable, indent=2), encoding="utf-8")

if __name__ == "__main__":
    main()
```

Uso:
```bash
python .harness/scripts/context_manager.py
```

**Beneficio**: el agente (o el usuario) puede ejecutarlo periódicamente y ver si está en zona de riesgo.

---

### 3.4. Skills ejecutables

**Problema**: las skills son markdown, no se pueden invocar como herramientas.

**Solución**: cada skill tiene un script ejecutable.

```
.harness/
├── skills/
│   ├── coding-standards/
│   │   ├── SKILL.md          # documentacion
│   │   └── run.sh            # script ejecutable
│   ├── security-audit/
│   │   ├── SKILL.md
│   │   ├── run.sh
│   │   └── checks/
│   │       ├── secrets.sh
│   │       └── patterns.sh
│   ├── test-generation/
│   │   ├── SKILL.md
│   │   └── generate_tests.py
│   └── ...
```

**Ejemplo de skill ejecutable**:

```bash
# .harness/skills/security-audit/run.sh
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "=== Security Audit ==="
bash "$SCRIPT_DIR/checks/secrets.sh"
bash "$SCRIPT_DIR/checks/patterns.sh"
echo "=== Audit complete ==="
```

```bash
# .harness/skills/security-audit/checks/secrets.sh
#!/usr/bin/env bash
# Busca API keys reales en archivos trackeados.
PATTERNS=(
    'sk-ant-[A-Za-z0-9_-]{20,}'
    'sk-[A-Za-z0-9]{30,}'
    'AIza[0-9A-Za-z_-]{35}'
    'gh[oprsu]_[A-Za-z0-9_]{36,}'
)
for pattern in "${PATTERNS[@]}"; do
    matches=$(git ls-files | xargs grep -lE "$pattern" 2>/dev/null || true)
    if [ -n "$matches" ]; then
        echo "  [FAIL] Pattern '$pattern' found in: $matches"
    else
        echo "  [OK] No matches for '$pattern'"
    fi
done
```

**Beneficio**: el agente puede invocar `bash .harness/skills/security-audit/run.sh` y obtener resultados estructurados.

---

## 4. Mejoras de largo plazo (refactor del harness)

### 4.1. Estado estructurado en lugar de JSON mezclado

**Problema**: `current-progress.json` mezcla estado efímero, decisiones, estadísticas, etc.

**Solución**: separar en archivos temáticos.

```
.harness/memory/
├── state.json              # estado actual (status, current_task, next_task)
├── decisions.md            # decisiones arquitectonicas con fecha
├── stats.json              # metricas acumuladas (commits, tests, etc.)
├── issues/
│   ├── 1.json              # datos del issue 1
│   └── 2.json              # datos del issue 2
└── history/
    ├── 2026-07-18-session-1.md
    └── 2026-07-19-session-2.md
```

**Beneficio**: cada archivo tiene una responsabilidad clara. Versionables independientemente. Más fáciles de debuggear.

### 4.2. Plan ejecutable (no solo markdown)

**Problema**: el plan se desvía y no hay un mecanismo para reconciliar.

**Solución**: el tech plan es un JSON o YAML con estado.

```yaml
# .harness/tech/tech-plan.yaml
id: 2
title: "Java/Spring Boot/Angular migration"
status: in_progress
created: 2026-07-19
tasks:
  - id: 0
    title: "Refactor compartido"
    status: done
    commit: 26bf7a5
    started: 2026-07-19T08:33:00Z
    completed: 2026-07-19T08:35:00Z
    deviations:
      - "Se decidio usar Java 21 (no 17) por consistencia con el stack"
  - id: 1
    title: "Setup Spring Boot"
    status: done
    commit: 92d6eb1
    deviations: []
  - id: 2
    title: "Capa de dominio"
    status: done
    commit: 0a90cc2
    deviations:
      - "Se anadio .gitignore local por subcarpeta"
  - id: 5
    title: "Adaptador LLM"
    status: done
    commit: 0b8af0c
    deviations:
      - reason: "Spring AI 1.x tiene bug con Anthropic"
        change: "Reemplazado por HTTP directo con RestClient"
        impact: "Se elimino la dependencia spring-ai-starter-model-anthropic"
  # ...
```

**Beneficio**: el agente puede leer el estado real (qué tasks están done, qué commits las cierran, qué deviations hubo) sin parsear markdown.

### 4.3. Reviewer real (con checklist)

**Problema**: el "Reviewer" no existe, no hay validación automatizada.

**Solución**: un agente Reviewer con checklist explícito.

```markdown
# Skill: code-review

## Checklist para revisar un commit:

### Seguridad
- [ ] No hay secrets en el diff (sk-ant-, sk-, AIza, gh*, AKIA)
- [ ] No hay archivos .env, .pem, id_rsa staged
- [ ] Las queries SQL usan parametros (?) no concatenacion
- [ ] No hay eval(), exec(), shell=True innecesarios
- [ ] No hay hardcoded URLs con credenciales

### Calidad
- [ ] Los tests nuevos pasan
- [ ] Hay tests para el codigo nuevo (no solo codigo)
- [ ] Linter pasa (ruff, spotless, eslint)
- [ ] Mensajes de commit siguen Conventional Commits
- [ ] No se commitean archivos generados (target/, dist/, .angular/)

### Arquitectura
- [ ] Respeta la estructura de capas (hexagonal si aplica)
- [ ] No hay imports cruzados indebidos (domain <- infrastructure)
- [ ] Interfaces (puertos) se mantienen en domain/
- [ ] Adaptadores implementan puertos, no al reves
```

**Beneficio**: cada PR pasa por un checklist explícito. La calidad sube.

### 4.4. Postmortem automatico al cerrar Issue

**Problema**: no se aprende de los errores. Se repiten.

**Solución**: al cerrar un Issue, el agente genera un postmortem.

```markdown
# .harness/postmortems/issue-2-java-migration.md

## Lo que funciono bien
- Arquitectura hexagonal estricta fue mas facil de lo esperado
- Hexagonal no se sintio como over-engineering
- Tests con H2 para JDBC fueron suficientes
- Frontend Angular + signals es muy elegante

## Lo que fallo
- Spring AI 1.x tiene bug con Anthropic (cambio a HTTP directo)
- Tests E2E no funcionan sin API key real (deberian tener mock)
- Plan no se actualizo cuando hubo deviations

## Lecciones aprendidas
- Verificar versiones de librerias antes de comprometerse
- Para demos, no usar features avanzadas de Spring AI (usar HTTP directo)
- Documentar deviations en el plan, no solo en commits

## Tiempo
- Estimado: 7h
- Real: 6h (mejor de lo esperado)
- Bloqueos: 1 (bug Spring AI, ~30 min)
```

**Beneficio**: el siguiente proyecto parte de estas lecciones.

---

## 5. Cambios de diseño del harness

### 5.1. Separar plan mode y execution mode

**Problema**: el agente mezcla "pensar el plan" con "ejecutar".

**Solución**: un flag `--mode` en el orquestador.

```bash
# Modo plan: solo modifica .harness/
./harness.sh --mode=plan
> "Voy a analizar el issue y proponer un plan"

# Modo ejecucion: solo modifica codigo.
./harness.sh --mode=exec
> "Implementando task 3: capa de aplicacion"
```

**Beneficio**: menos confusión, menos riesgo de inconsistencias.

### 5.2. Checkpoints automaticos con "dry run"

**Problema**: el agente commitea directamente sin verificar.

**Solución**: dry-run mode que simula todo y muestra el diff antes de commitear.

```bash
./harness.sh --mode=exec --dry-run
> "Esto es lo que VA A PASAR si apruebas:"
> "  - Modificar src/foo.py (línea 42: agregar 'x = 5')"
> "  - Commit: 'feat: add x'"
> "  - Push a origin/main"
> "Para ejecutar de verdad: ./harness.sh --mode=exec"
```

**Beneficio**: el usuario sabe exactamente qué va a pasar antes de que pase.

---

## 6. Skills que faltan

Basado en lo que pasó en este proyecto, hacen falta skills para:

| Skill | Por qué | Esfuerzo |
|---|---|---|
| **dependency-audit** | Verificar CVEs de Python, Java, Node | 1 día |
| **migration-planner** | Planificar migraciones (ej: Python → Java) | 1 día |
| **docker-setup** | Crear docker-compose para nuevos proyectos | 0.5 días |
| **cicd-setup** | Crear GitHub Actions para nuevos proyectos | 0.5 días |
| **openapi-gen** | Generar OpenAPI/Swagger desde controllers Java | 0.5 días |
| **test-data-gen** | Generar seed data realista para tests | 1 día |
| **i18n-setup** | Setup i18n en un nuevo proyecto (Python/Angular) | 0.5 días |
| **auth-setup** | Añadir autenticación (JWT, Spring Security) | 1 día |
| **observability** | Logging, métricas, health checks | 1 día |

Cada skill tendría:
- `SKILL.md` (documentación, qué hace, cuándo usarla)
- Script ejecutable
- Tests del propio skill (sí, los skills también se testean)

---

## 7. Configuración del orquestador mejorada

El `.harness/agents.md` actual es muy vago. Una versión mejorada:

```markdown
---
role: orchestrator
mode: autonomous-with-checkpoints
description: >
  Orquestador ejecutable. Lee el estado del repo y decide que hacer.
  Actua de forma autonoma hasta llegar a un checkpoint donde consulta al usuario.
checkpoints:
  - al iniciar un feature nuevo
  - al aprobar research
  - al aprobar plan
  - al completar cada task
  - al cerrar un issue
agent_states:
  - idle: no hay feature activa
  - research: gathering requirements
  - planning: disennando tasks
  - implementing: ejecutando tasks
  - reviewing: validando trabajo
  - done: feature completa
---

# Orquestador

## Cuando el usuario dice "nueva feature X":

1. **idle → research**: crea Issue en GitHub. Hace preguntas de clarificacion.
   Al confirmar: genera `.harness/research/research-plan.md` y transiciona a `awaiting_research_approval`.

2. **awaiting_research_approval**: notifica al usuario. Al aprobar:
   transiciona a planning.

3. **planning → in_progress**: genera `.harness/tech/tech-plan.yaml` con tasks
   atomicas. Transiciona a `awaiting_plan_approval`.

4. **awaiting_plan_approval**: notifica al usuario. Al aprobar:
   transiciona a implementing.

5. **implementing**: para cada task pendiente:
   - Lee la task del plan
   - Verifica criterios de aceptacion
   - Implementa (writes code)
   - Verifica (tests, lint)
   - Commit con Conventional Commits
   - Marca task como done
   - Al completar todas: transiciona a reviewing.

6. **reviewing**: ejecuta el Reviewer (skill). Si pasa: done. Si falla:
   vuelve a implementing.

7. **done**: postmortem automatico, cerrar issue, transiciona a idle.

## Reglas duras

- NUNCA commitear un secret (usar el skill security-audit antes de cada commit)
- NUNCA modificar .env (solo .env.example)
- NUNCA saltarse tests
- SIEMPRE mantener el plan actualizado cuando hay deviations
- SIEMPRE generar postmortem al cerrar un issue
```

---

## 8. Plantillas de documentos mejoradas

### 8.1. research-plan.md mejorado

```markdown
# Research Plan — <feature>

## Metadata
- issue: <numero>
- created: <YYYY-MM-DD>
- status: draft | awaiting_approval | approved
- estimated_complexity: SIMPLE | MEDIUM | COMPLEX

## Objetivo
<que construimos y por que, max 3 lineas>

## Contexto y Restricciones
- <dependencias, limites explicitos, que NO haremos>
- <requisitos no funcionales: performance, seguridad, etc.>

## Decisiones de Diseno
- Decision 1: <opcion elegida> - <razon>
- Decision 2: ...

## Trade-offs Considerados
| Opcion | Pros | Contras | Decision |
|---|---|---|---|
| A | ... | ... | elegimos A |
| B | ... | ... | descartada |

## Riesgos Identificados
- Riesgo 1: <descripcion> - Mitigacion: <como>

## Acceptance Criteria
- [ ] Criterio 1 (medible)
- [ ] Criterio 2 (medible)

## Stack del Proyecto
- Lenguaje:
- Tests:
- Lint:
```

### 8.2. tech-plan.yaml (YAML en vez de markdown)

Ya lo vimos arriba en #4.2. La ventaja de YAML es que es **parseable** y se puede validar programaticamente.

### 8.3. postmortem.md

```markdown
# Postmortem — <feature>

## Metadata
- issue: <numero>
- started: <YYYY-MM-DD>
- completed: <YYYY-MM-DD>
- estimated_hours: <X>
- actual_hours: <Y>
- deviation: <Y - X>

## Summary
<1-2 parrafos de que se hizo>

## Lo que funciono bien
- <punto 1>
- <punto 2>

## Lo que fallo
- <punto 1>
- <punto 2>

## Lecciones aprendidas
- <leccion 1>
- <leccion 2>

## Que haria diferente la proxima vez
- <accion 1>
- <accion 2>

## Tiempo gastado por task
- Task 1: Xh (estimado Yh)
- Task 2: Xh (estimado Yh)
- ...
```

---

## 9. Checklist pre-merge

Crea `.github/PULL_REQUEST_TEMPLATE.md`:

```markdown
## Descripcion
<que cambia este PR y por que>

## Referencia
- Issue: #<numero>
- Plan: `.harness/tech/tech-plan.yaml#task-<N>`

## Checklist del Reviewer (auto-evaluacion del autor)

### Seguridad
- [ ] `bash .harness/skills/security-audit/run.sh` pasa
- [ ] No he commiteado secrets (verificado con `git diff --staged`)
- [ ] `.env` no esta en el diff

### Calidad
- [ ] Tests pasan localmente (`./harness.sh test`)
- [ ] Linter pasa (ruff para Python, mvn spotless:check para Java, npm run lint para Angular)
- [ ] He actualizado la documentacion afectada

### Arquitectura
- [ ] Respeta la estructura de capas (hexagonal)
- [ ] No hay imports cruzados indebidos
- [ ] Si he cambiado el plan, lo he actualizado

### Postmortem
- [ ] Si esto es un cierre de feature, he generado un postmortem en `.harness/postmortems/`
```

---

## 10. Lecciones aprendidas

De este proyecto, las 10 lecciones mas importantes para el siguiente:

### 1. **Empieza por el dominio, no por la infraestructura**
En este proyecto, Task 2 (dominio) se hizo ANTES de los adaptadores externos. Eso fue clave: cuando Task 5 (LLM) tuvo un problema con Spring AI, el cambio a HTTP directo fue trivial porque el dominio no dependia de Spring AI. **Regla**: domain/ es lo primero, adaptadores son reemplazables.

### 2. **Hexagonal no es over-engineering**
Lo parecia para una app pequeña, pero al migrar de Python a Java, fue la diferencia entre un port de 2 horas y una reescritura de 1 semana. La inversion se amortiza. **Regla**: usa hexagonal desde el dia 1 en cualquier proyecto que esperes mantener mas de 3 meses.

### 3. **Validacion de SQL es una feature de seguridad, no de funcionalidad**
El validador de SQL no es "nice to have", es **defense in depth**. Si alguien bypasea el LLM, el validador es la red de seguridad. Ademas, anadir el usuario readonly en la BD es una tercera capa. **Regla**: nunca confies en una sola capa de validación.

### 4. **Los tests E2E con API real son fragiles**
En este proyecto los tests E2E de Python fallan porque la API key se revoco. Para demos, mockea la API externa. **Regla**: tests E2E con servicios externos solo en CI (no local), y con secrets en GitHub Secrets.

### 5. **El .env NO es opcional, es obligatorio desde el dia 1**
Lo aprendimos en el Issue #1 (tuvimos que añadirlo cuando empezamos con Claude). Si lo hubieras pensado desde el principio, te habrias ahorrado varios commits. **Regla**: el primer commit de cualquier proyecto nuevo incluye .env.example + .gitignore con .env.

### 6. **Las dos implementaciones (Python + Java) son valiosas, pero no copies 1:1**
La tentación fue hacer "lo mismo pero en Java". La realidad es que cada stack tiene su idiom: hexagonal funciona en Java pero es overkill en Python, Streamlit no tiene equivalente en Angular, etc. **Regla**: para migrar, entiende el equivalente conceptual, no copies el codigo.

### 7. **El harness da estructura pero no autonomia**
En este proyecto, tu (humano) fuiste el orquestador. El harness documento que habia que hacer pero no lo hizo. **Regla**: si quieres autonomia real, necesitas herramientas ejecutables (no documentos). Ver seccion 3.2.

### 8. **Spring AI 1.x tiene bugs con Anthropic**
Perdimos ~30 min en esto. **Regla**: antes de adoptar una libreria nueva que envuelve un servicio que ya conoces, verifica que el caso basico funciona con un test rapido (5-10 min). Si no, ve directo a HTTP/SDK nativo.

### 9. **El audit de seguridad NO es opcional al final**
Lo hicimos "al final" y casi se nos cuela un secret en el commit. **Regla**: el audit de seguridad debe ser automatico (pre-commit hook + CI) desde el dia 1.

### 10. **Un README que nadie lee no sirve**
El README actual es largo (300 lineas). Un reclutador mira los primeros 5 bullets. **Regla**: el README debe tener un "TL;DR" arriba con 5 bullets maximo, y los detalles abajo.

---

## Apéndice: Implementacion recomendada paso a paso

```
Dia 1 (2-3 horas):
  1. Instalar pre-commit + .pre-commit-config.yaml
  2. Crear .github/workflows/ci.yml
  3. Crear .github/dependabot.yml
  4. Anadir JaCoCo al pom.xml
  5. Ejecutar todo y verificar que pasa

Dia 2 (4-6 horas):
  1. Implementar harness.sh (orquestador ejecutable)
  2. Crear skill security-audit ejecutable
  3. Convertir al menos 2 skills a ejecutables (coding-standards, test-generation)
  4. Crear el Context Manager
  5. Migrar el tech-plan de markdown a YAML

Semana 2 (2-3 dias):
  1. Refactor de la memoria a archivos tematicos
  2. Implementar el Reviewer agent con checklist
  3. Crear sistema de postmortem automatico
  4. Documentar todo en .harness/CONTRIBUTING.md (como contribuir al harness)
```

---

## Resumen ejecutivo para tu próximo repo

**TL;DR**: Las 4 cosas que mas impacto tienen y menos esfuerzo cuestan:

1. **`.github/workflows/ci.yml`** (1h) → bloquea bugs en cada PR
2. **`.pre-commit-config.yaml`** (30m) → bloquea secrets localmente
3. **`.github/dependabot.yml`** (10m) → te avisa de CVEs
4. **`harness.sh` ejecutable** (3h) → convierte el orquestador de docs a herramienta

Si solo tienes 2 horas, haz las 3 primeras. Si tienes medio día, haz las 4. Con eso ya tienes un harness 5x mejor que el actual.

---

**Autor del análisis**: sesión de revisión post-proyecto
**Fecha**: 2026-07-19
**Basado en**: 2 features end-to-end completadas (Issues #1 y #2)
**Repo analizado**: https://github.com/alvarobozser/sql-generator-consultant
