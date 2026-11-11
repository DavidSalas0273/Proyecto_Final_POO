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
| `transaction_api` | `POST /transfer` | RabbitMQ `transfer_commands` | 8080 |
| `transaction_processor` | RabbitMQ `transfer_commands` | PostgreSQL + Kafka `transactions_log` | — |
| `fraud_detector` | Kafka `transactions_log` | PostgreSQL + Kafka `fraud_alerts` | — |
| `dashboard_aggregator` | Kafka `transactions_log` y `fraud_alerts` | WebSocket `ws://localhost:8081/ws`, `GET /api/transactions` | 8081 |
| `notification_router` | Kafka `transactions_log` | RabbitMQ `email_queue` | — |
| `ad_generator` | Tarea programada (cada 30 s) | RabbitMQ `email_queue` | — |
| `email_worker` (3 réplicas) | RabbitMQ `email_queue` | Consola: `ENVIANDO EMAIL a [to]: [body]` | — |
| `dashboard/dashboard.html` | WebSocket del agregador | Navegador (sospechosas en rojo) | — |

## Diseño orientado a objetos

Cada servicio sigue una arquitectura hexagonal: `domain` (modelo y contratos), `application` (casos de uso y **puertos**) e `infrastructure` (adaptadores de RabbitMQ, Kafka, JPA, WebSocket y configuración).

- **Modelo rico**: `Transaccion`, `CuentaBancaria` y `VistaMaterializada` encapsulan su estado y lo cambian solo con métodos semánticos (`debitar()`, `acreditar()`, `ejecutarTransferencia()`, `rechazarPorFondosInsuficientes()`, `marcarComoSospechosa()`). No tienen setters.
- **State**: `EstadoTransaccion` con `EstadoRecibida → EstadoProcesando → EstadoCompletada → EstadoSospechosa` y `EstadoProcesando → EstadoRechazada`. Cada estado solo sobrescribe las transiciones que permite; las demás lanzan `IllegalStateException`, así que no hay `if/else` ni `switch` sobre el estado.
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

Envía una transferencia normal y otra sospechosa:

```bash
curl -X POST http://localhost:8080/transfer -H "Content-Type: application/json" -d '{"from_user":"A","to_user":"B","amount":50}'
```

```bash
curl -X POST http://localhost:8080/transfer -H "Content-Type: application/json" -d '{"from_user":"C","to_user":"D","amount":20000}'
```

Abre `dashboard/dashboard.html` en el navegador. Tiene un formulario para enviar transferencias y un botón que simula una de $20,000, que aparece en rojo.

Para ver el balanceo de carga entre las réplicas de `email_worker`:

```bash
docker compose logs -f email_worker
```

Para consultar la auditoría en PostgreSQL:

```bash
docker exec -it postgres-db psql -U ledger -d ledger -c "SELECT * FROM ledger_transacciones; SELECT * FROM alertas_fraude;"
```

La consola de RabbitMQ está en http://localhost:15672 (usuario `ledger`, contraseña `ledger`).

Notas:
- Las cuentas nuevas se abren con un saldo inicial de 100,000 USD (`LEDGER_SALDO_INICIAL`), así que una transferencia mayor a ese saldo se rechaza con `INSUFFICIENT_FUNDS`.
- Cada vez que arranca, el `dashboard_aggregator` reconstruye su vista en memoria reproduciendo los topics de Kafka desde el inicio.

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
