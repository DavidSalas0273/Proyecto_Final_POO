# Plataforma de Transacciones Bancarias (Ledger de Eventos)

Núcleo de un sistema bancario P2P con arquitectura **CQRS / Event Sourcing**:

- **RabbitMQ** es el *Command Bus* (Work Queues `transfer_commands` y `email_queue`).
- **Apache Kafka** es el *Event Bus* y el ledger inmutable (`transactions_log`, `fraud_alerts`), con `tx_id` como key.
- **PostgreSQL** guarda el histórico del ledger, los saldos y la auditoría de fraude.
- **Java 17 + Spring Boot 3.2** en los 7 microservicios.

```
POST /transfer ──► transaction_api ──► [RabbitMQ: transfer_commands] ──► transaction_processor ──► PostgreSQL
                                                                                 │
                                                                                 ▼
                                                                  [Kafka: transactions_log]
                                       ┌─────────────────────────────┬───────────┴──────────────┐
                                       ▼                             ▼                          ▼
                              dashboard_aggregator            fraud_detector ──► PostgreSQL   notification_router
                                 ▲        │                          │                          │
                                 │        ▼ WebSocket :8081          ▼                          ▼
                [Kafka: fraud_alerts]  dashboard.html      [Kafka: fraud_alerts]      [RabbitMQ: email_queue] ◄── ad_generator
                                                                                                │
                                                                                                ▼
                                                                                   email_worker (x3 réplicas)
```

## Microservicios

| Servicio | Entrada | Salida | Puerto |
|---|---|---|---|
| `transaction_api` | `POST /transfer` (enviar) y `POST /deposit` (recargar) | RabbitMQ `transfer_commands` | 8080 |
| `transaction_processor` | RabbitMQ `transfer_commands` | PostgreSQL + Kafka `transactions_log` | — |
| `fraud_detector` | Kafka `transactions_log` | PostgreSQL + Kafka `fraud_alerts` | — |
| `dashboard_aggregator` | Kafka `transactions_log` y `fraud_alerts` | WebSocket `ws://localhost:8081/ws`, `GET /api/transactions`, `GET /api/accounts` | 8081 |
| `notification_router` | Kafka `transactions_log` | RabbitMQ `email_queue` | — |
| `ad_generator` | Tarea programada (cada 30 s) | RabbitMQ `email_queue` | — |
| `email_worker` (3 réplicas) | RabbitMQ `email_queue` | Consola: `ENVIANDO EMAIL a [to]: [body]` | — |
| `dashboard/dashboard.html` | WebSocket del agregador | Navegador: billetera, cuentas y movimientos (sospechosas en rojo) | — |

## Saldos, recargas y envíos

- Cada usuario tiene una cuenta con saldo. Las cuentas nuevas se abren en **$0** (`LEDGER_SALDO_INICIAL`).
- **Recargar** (`POST /deposit`) suma el monto al saldo del usuario.
- **Enviar** (`POST /transfer`) resta el monto al que envía y lo suma al que recibe.
- Si el que envía no tiene saldo suficiente, el movimiento queda **DENEGADO** (`REJECTED`, motivo `INSUFFICIENT_FUNDS`):
  - no se mueve dinero;
  - el movimiento queda registrado en el ledger y en la lista de movimientos del dashboard;
  - el emisor recibe un correo avisándole.
- Las recargas y las transferencias viajan por la misma cola (`transfer_commands`), con un campo `type` (`DEPOSIT` o `TRANSFER`), para que se procesen en el orden en que se pidieron.
- Cada evento del ledger lleva el saldo resultante de las cuentas involucradas (`from_balance` y `to_balance`). Con eso, el dashboard muestra los saldos en tiempo real sin consultar la base de datos.

## Dashboard

- **Mi billetera:** eliges el usuario activo y ves su saldo.
  - Tiene dos pestañas, *Enviar* y *Recargar*, esta última con montos rápidos.
  - Si el monto supera el saldo, avisa antes de enviar que el sistema lo denegará.
- **Cuentas:** muestra el saldo de cada usuario. Al hacer clic en una cuenta, pasa a ser el usuario activo.
- **Movimientos:**
  - filtros (Todos, Envíos, Recargas, Denegados, Sospechosos), buscador y la opción "Solo los míos";
  - los movimientos denegados se ven tachados y en naranja, con el motivo;
  - los sospechosos se resaltan en rojo.
- **Notificaciones emergentes:** confirman cuándo termina cada operación y avisan de las alertas de fraude.
- **Escenarios de demostración:**
  - transferencia sospechosa de $20.000, que primero recarga la cuenta de C;
  - envío desde una cuenta sin saldo.

## Diseño orientado a objetos

Cada servicio sigue una arquitectura hexagonal: `domain` (modelo y contratos), `application` (casos de uso y **puertos**) e `infrastructure` (adaptadores de RabbitMQ, Kafka, JPA, WebSocket y configuración).

- **Modelo rico**: `Transaccion`, `CuentaBancaria` y `VistaMaterializada` encapsulan su estado y lo cambian solo con métodos semánticos (`debitar()`, `acreditar()`, `ejecutarTransferencia()`, `ejecutarRecarga()`, `rechazarPorFondosInsuficientes()`, `marcarComoSospechosa()`). No tienen setters.
- **Polimorfismo sin `switch`**:
  - `TipoMovimiento` (`TRANSFER`/`DEPOSIT`) define en cada constante cómo se mueve el dinero.
  - Cada comando (`TransferCommandRecord`, `DepositCommandRecord`) construye su propio agregado.
  - Las plantillas de correo se eligen con una tabla de estrategias por `TIPO:ESTADO`.
- **State**: `EstadoTransaccion` con `EstadoRecibida → EstadoProcesando → EstadoCompletada → EstadoSospechosa` y `EstadoProcesando → EstadoRechazada`. Cada estado solo sobrescribe las transiciones que permite; las demás lanzan `IllegalStateException`, así que no hay `if/else` ni `switch` sobre el estado.
  - El ciclo se cumple completo en ejecución. Cuando `fraud_detector` publica una alerta, `transaction_processor` la consume en `fraud_alerts` y reconstituye la `Transaccion` desde PostgreSQL. Luego aplica `marcarComoSospechosa()`, registra el nuevo estado en `historial_estados` y publica en el ledger un evento `SUSPICIOUS`.
  - El asiento de `ledger_movimientos` nunca se modifica: los cambios de estado se agregan como filas nuevas al historial.
- **`RegistroAuditoria`** (en `fraud_detector`): entidad rica de auditoría forense.
  - Solo se puede abrir sobre una transferencia completada, y exige motivo y fecha.
  - Es inmutable y genera la alerta pública con `aAlerta()`.
- **Strategy**:
  - Validación de transacciones (`transaction_processor`): `DivisaCompatibleRegla`, `LimitePorTransaccionRegla` y `FondosSuficientesRegla`, combinadas en `CadenaValidacion`.
  - Detección de fraude (`fraud_detector`): `TransaccionAltoValorRegla` (> $10,000) y `FrecuenciaAnomalaRegla` (stateful, ventana deslizante), combinadas en `PoliticaDeteccionFraude`.
- **DIP**: inyección solo por constructor. Los casos de uso dependen de interfaces (`TransferCommandPublisher`, `PublicadorEventosLedger`, `TransaccionRepository`, `CuentaRepository`, `RepositorioAlertasFraude`, `DifusorDashboard`, ...).
- **Value Objects con `record`**: `TransferCommandRecord`, `TransactionEventRecord`, `FraudAlertRecord`, `EmailCommandRecord`, `MontoDinero` y `TransactionId`. Sus constructores compactos validan con *fail-fast* (montos ≤ 0, divisas no admitidas, identificadores vacíos).
- **Persistencia desacoplada**: las entidades JPA (`TransaccionJpaEntity`, `CuentaJpaEntity`, `AlertaFraudeJpaEntity`) están separadas del dominio y se traducen en adaptadores. El ledger y las alertas son `@Immutable` (solo inserción); las cuentas usan bloqueo optimista con `@Version`.

## Ejecución

Requisitos: Docker Desktop (con Docker Compose v2).

```bash
docker compose up -d --build
```

Recarga saldo a A y luego envía $50 a B:

```bash
curl -X POST http://localhost:8080/deposit -H "Content-Type: application/json" -d '{"user":"A","amount":200}'
```

```bash
curl -X POST http://localhost:8080/transfer -H "Content-Type: application/json" -d '{"from_user":"A","to_user":"B","amount":50}'
```

Intenta un envío sin saldo suficiente, que quedará denegado:

```bash
curl -X POST http://localhost:8080/transfer -H "Content-Type: application/json" -d '{"from_user":"B","to_user":"A","amount":500}'
```

Abre `dashboard/dashboard.html` en el navegador para operar desde la interfaz.

Para ver el balanceo de carga entre las réplicas de `email_worker`:

```bash
docker compose logs -f email_worker
```

Para consultar la auditoría en PostgreSQL:

```bash
docker exec -it postgres-db psql -U ledger -d ledger -c "SELECT * FROM ledger_movimientos; SELECT * FROM historial_estados; SELECT * FROM cuentas_bancarias; SELECT * FROM alertas_fraude;"
```

La consola de RabbitMQ está en http://localhost:15672 (usuario `ledger`, contraseña `ledger`).

Notas:
- Si vienes de una versión anterior del proyecto, reinicia los datos con `docker compose down -v`. Antes las cuentas empezaban con 100.000 y el ledger usaba otra tabla.
- Cada vez que arranca, el `dashboard_aggregator` reconstruye su vista en memoria reproduciendo los topics de Kafka desde el inicio.

## Desarrollo desde IntelliJ IDEA (Run / Debug)

1. Abre el proyecto: **File → Open**, elige el `pom.xml` raíz y luego **Open as Project**. Usa el **SDK 17**.
2. Levanta solo la infraestructura. Puedes usar la configuración **Infraestructura (Docker)** o ejecutar:

   ```bash
   docker compose -f docker-compose.infra.yml up -d
   ```

3. Elige **Ledger - todos los servicios** en el selector de configuraciones y pulsa ▶ (o 🐞 para depurar). También puedes arrancar un servicio suelto con su propia configuración.
4. Abre `dashboard/dashboard.html` en el navegador.

Las configuraciones están en la carpeta `.run/` y IntelliJ las detecta solas. Los servicios usan los valores por defecto de cada `application.yml`:

| Servicio | Dirección | Credenciales |
|---|---|---|
| PostgreSQL | `localhost:5433` | `ledger`/`ledger` |
| Kafka | `localhost:9092` | — |
| RabbitMQ | `localhost:5672` | `ledger`/`ledger` |

PostgreSQL usa el 5433 para no chocar con un PostgreSQL instalado en Windows.

No ejecutes `docker-compose.infra.yml` y `docker-compose.yml` a la vez, porque usan los mismos puertos.

## Decisiones de interpretación del enunciado

- **Publicidad y fan-out.** Los objetivos de aprendizaje dicen que *publicidad* reacciona a los eventos del ledger, pero la especificación de `ad_generator` lo define como una *tarea programada periódica*. Se siguió la especificación del microservicio: `ad_generator` usa `@Scheduled` y comparte con `notification_router` la cola `email_queue`. El fan-out sobre Kafka lo hacen `dashboard_aggregator`, `fraud_detector` y `notification_router`, como describen los flujos 1 y 2.
- **Correos de transferencia.** Usan el formato exacto del ejemplo (`"Envió $50 a B"`). Los correos de recarga y de envío denegado, que el enunciado no define, sí incluyen el saldo disponible.

## Pruebas unitarias

Las pruebas son JUnit 5 + Mockito con el patrón AAA. No arrancan Spring, ni bases de datos, ni brokers. Para ejecutarlas todas sin tener Maven instalado:

```bash
docker run --rm -v "$(pwd)":/app -w /app maven:3.9.6-eclipse-temurin-17-alpine mvn -B test
```

Con Maven local basta con `mvn test` desde la raíz (el `pom.xml` raíz agrega los 7 módulos).

## Contenedores

- **Dockerfile multietapa** en cada servicio:
  - Etapa de build: `maven:3.9.6-eclipse-temurin-17-alpine`, con `dependency:go-offline` antes de copiar el código para aprovechar la caché de capas.
  - Etapa de runtime: `eclipse-temurin:17-jre-alpine`, con el usuario no privilegiado `appuser:appgroup`.
  - Flags de la JVM: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`.
- **docker-compose.yml**:
  - Límites de CPU y memoria en `deploy.resources.limits` para todos los contenedores.
  - `deploy.replicas: 3` para `email_worker`.
  - Volumen `pgdata` y *healthchecks* para PostgreSQL, Kafka, Zookeeper y RabbitMQ.
  - Tres redes:
    - `public-net`: la única con puertos publicados al host.
    - `messaging-net`: interna, para Kafka, Zookeeper y RabbitMQ.
    - `persistence-net`: interna, solo para PostgreSQL y los servicios que persisten.
