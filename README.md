
# Plataforma de Transacciones Bancarias

Sistema de transacciones bancarias basado en una arquitectura de microservicios que implementa los patrones **CQRS (Command Query Responsibility Segregation)** y **Event Sourcing**. Utiliza RabbitMQ para gestionar comandos, Apache Kafka para distribuir eventos y PostgreSQL para almacenar la información de las cuentas, los movimientos y las alertas de fraude.

El proyecto está desarrollado en Java 17 con Spring Boot 3.2 y está compuesto por siete microservicios independientes.

## Tabla de contenidos

- [Arquitectura](#arquitectura)
- [Microservicios](#microservicios)
- [Funcionamiento del sistema](#funcionamiento-del-sistema)
- [Dashboard](#dashboard)
- [Diseño y patrones de software](#diseño-y-patrones-de-software)
- [Tecnologías](#tecnologías)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Ejecución desde IntelliJ IDEA](#ejecución-desde-intellij-idea)
- [Pruebas unitarias](#pruebas-unitarias)
- [Configuración de Docker](#configuración-de-docker)
- [Decisiones de implementación](#decisiones-de-implementación)

## Arquitectura

El sistema distribuye las responsabilidades entre distintos microservicios que se comunican mediante mensajería asíncrona.

Los componentes principales son:

- **RabbitMQ:** gestiona los comandos mediante las colas `transfer_commands` y `email_queue`.
- **Apache Kafka:** distribuye los eventos del sistema a través de los topics `transactions_log` y `fraud_alerts`. El identificador `tx_id` se utiliza como clave de los eventos.
- **PostgreSQL:** almacena las cuentas bancarias, los movimientos del ledger, el historial de estados y las alertas de fraude.
- **Spring Boot:** proporciona la base para desarrollar los siete microservicios.

### Flujo general

```text
POST /transfer
      |
      v
transaction_api
      |
      v
RabbitMQ: transfer_commands
      |
      v
transaction_processor
      |
      +------> PostgreSQL
      |
      v
Kafka: transactions_log
      |
      +-------------------+-------------------+
      |                   |                   |
      v                   v                   v
dashboard_aggregator  fraud_detector   notification_router
      |                   |                   |
      v                   v                   v
  Dashboard            PostgreSQL       RabbitMQ: email_queue
  WebSocket                |                   ^
                          Kafka                |
                      fraud_alerts        ad_generator
                          |                   |
                          v                   |
                  transaction_processor      |
                                              v
                                      email_worker (x3)
```

El procesamiento de las operaciones se realiza de forma asíncrona. Una vez publicada una transacción en Kafka, los servicios interesados reciben el evento y ejecutan sus respectivas tareas sin depender directamente unos de otros.

## Microservicios

El sistema está dividido en siete microservicios, cada uno con una responsabilidad específica.

| Microservicio | Responsabilidad | Puerto |
|---|---|---|
| `transaction_api` | Recibe solicitudes de transferencias y recargas. | 8080 |
| `transaction_processor` | Procesa los comandos, actualiza los saldos y publica eventos. | — |
| `fraud_detector` | Analiza las transacciones y genera alertas de fraude. | — |
| `dashboard_aggregator` | Construye la información del dashboard y gestiona las actualizaciones en tiempo real. | 8081 |
| `notification_router` | Genera comandos de notificación a partir de los eventos. | — |
| `ad_generator` | Genera mensajes publicitarios mediante una tarea programada. | — |
| `email_worker` | Procesa los mensajes de la cola de correos. Cuenta con tres réplicas. | — |

### Comunicación entre servicios

| Componente | Entrada | Salida |
|---|---|---|
| `transaction_api` | Solicitudes HTTP | RabbitMQ |
| `transaction_processor` | RabbitMQ | PostgreSQL y Kafka |
| `fraud_detector` | Kafka: `transactions_log` | PostgreSQL y Kafka: `fraud_alerts` |
| `dashboard_aggregator` | Kafka: `transactions_log` y `fraud_alerts` | WebSocket y API REST |
| `notification_router` | Kafka: `transactions_log` | RabbitMQ |
| `ad_generator` | Tarea programada cada 30 segundos | RabbitMQ |
| `email_worker` | RabbitMQ | Consola |

## Funcionamiento del sistema

### Cuentas y movimientos

Cada usuario dispone de una cuenta bancaria cuyo saldo inicial es de **$0**, definido mediante la configuración `LEDGER_SALDO_INICIAL`.

El sistema permite realizar dos operaciones principales:

- **Recargas (`POST /deposit`):** incrementan el saldo de la cuenta indicada.
- **Transferencias (`POST /transfer`):** descuentan el monto de la cuenta de origen y lo acreditan en la cuenta de destino.

Las dos operaciones utilizan la misma cola de RabbitMQ (`transfer_commands`). Se diferencian mediante el campo `type`, que puede tomar los valores `DEPOSIT` o `TRANSFER`.

Esto permite centralizar el procesamiento de los comandos y conservar el orden de las solicitudes recibidas.

### Validación de fondos

Antes de completar una transferencia, el sistema verifica que la cuenta de origen disponga de saldo suficiente.

Cuando no se cumple esta condición, la operación queda registrada con estado `REJECTED` y motivo `INSUFFICIENT_FUNDS`.

En este escenario:

- No se modifica el saldo de ninguna cuenta.
- La operación queda registrada en el ledger.
- El movimiento aparece en el dashboard como denegado.
- Se genera una notificación por correo al usuario que intentó realizar la transferencia.

### Registro de eventos

Cada evento del ledger contiene información sobre los saldos resultantes de las cuentas involucradas, mediante los campos `from_balance` y `to_balance`.

Esta información permite que el dashboard actualice los saldos en tiempo real a partir de los eventos recibidos, sin necesidad de consultar PostgreSQL para cada actualización.

## Dashboard

El proyecto incluye una interfaz web ubicada en `dashboard/dashboard.html`, que permite interactuar con las cuentas y consultar los movimientos registrados.

La interfaz está organizada en tres secciones principales:

### Mi billetera

- Permite seleccionar el usuario activo.
- Muestra el saldo disponible.
- Incluye las opciones de enviar y recargar dinero.
- Ofrece montos predefinidos para las recargas.
- Advierte cuando se intenta transferir un monto superior al saldo disponible.

### Cuentas

- Muestra las cuentas registradas y sus respectivos saldos.
- Permite seleccionar una cuenta para convertirla en el usuario activo.

### Movimientos

- Consulta el historial de operaciones.
- Incluye filtros por tipo de movimiento: todos, envíos, recargas, denegados y sospechosos.
- Permite buscar movimientos y filtrar únicamente los correspondientes al usuario activo.
- Identifica visualmente las operaciones rechazadas y las transacciones sospechosas.

### Actualizaciones en tiempo real

El dashboard recibe información mediante WebSocket:

`ws://localhost:8081/ws`

También dispone de los siguientes endpoints REST:

- `GET /api/transactions`: consulta los movimientos.
- `GET /api/accounts`: consulta las cuentas y sus saldos.

La interfaz presenta notificaciones emergentes cuando una operación finaliza o se detecta una posible actividad fraudulenta.

### Escenarios de demostración

Se incluyen dos escenarios para comprobar el comportamiento del sistema:

1. **Detección de fraude:** transferencia sospechosa por $20.000. Para ejecutar este escenario, primero se recarga la cuenta C.
2. **Fondos insuficientes:** intento de transferencia desde una cuenta sin saldo suficiente.

## Diseño y patrones de software

Los microservicios siguen una arquitectura hexagonal, separando la lógica de negocio de los mecanismos de comunicación y persistencia.

La estructura se divide en tres capas:

- `domain`: contiene las entidades, reglas de negocio y contratos.
- `application`: implementa los casos de uso y los puertos.
- `infrastructure`: contiene los adaptadores de RabbitMQ, Kafka, JPA, WebSocket y las configuraciones.

### Modelo de dominio

Las entidades principales son `Transaccion`, `CuentaBancaria` y `VistaMaterializada`.

Cada una encapsula su propio estado y expone métodos específicos para realizar modificaciones, por ejemplo:

- `debitar()`
- `acreditar()`
- `ejecutarTransferencia()`
- `ejecutarRecarga()`
- `rechazarPorFondosInsuficientes()`
- `marcarComoSospechosa()`

Se evita el uso de setters públicos para mantener el control sobre las modificaciones del dominio.

### Patrones de diseño implementados

**Polimorfismo**

- `TipoMovimiento` define el comportamiento de cada operación (`TRANSFER` y `DEPOSIT`).
- `TransferCommandRecord` y `DepositCommandRecord` construyen sus respectivos agregados.
- Las plantillas de correo se seleccionan mediante una tabla de estrategias según la combinación `TIPO:ESTADO`.

**State**

El estado de las transacciones se representa mediante `EstadoTransaccion`.

Las transiciones implementadas son:

- `EstadoRecibida → EstadoProcesando`
- `EstadoProcesando → EstadoCompletada`
- `EstadoCompletada → EstadoSospechosa`
- `EstadoProcesando → EstadoRechazada`

Cada estado define las transiciones permitidas. Las operaciones no válidas generan una `IllegalStateException`.

El flujo de detección de fraude también contempla la actualización del estado de una transacción ya completada:

1. `fraud_detector` identifica una operación sospechosa y publica una alerta en `fraud_alerts`.
2. `transaction_processor` consume la alerta.
3. Recupera la transacción desde PostgreSQL.
4. Ejecuta `marcarComoSospechosa()`.
5. Registra el cambio en `historial_estados`.
6. Publica un nuevo evento `SUSPICIOUS` en el ledger.

El registro original de `ledger_movimientos` no se modifica. Los cambios de estado se conservan como nuevos registros en el historial.

**Strategy**

Se utiliza para implementar reglas de validación y detección de fraude.

En `transaction_processor`:

- `DivisaCompatibleRegla`
- `LimitePorTransaccionRegla`
- `FondosSuficientesRegla`

Estas reglas se combinan mediante `CadenaValidacion`.

En `fraud_detector`:

- `TransaccionAltoValorRegla`: identifica transacciones superiores a $10.000.
- `FrecuenciaAnomalaRegla`: analiza la frecuencia de operaciones mediante una ventana deslizante.

Ambas reglas se integran en `PoliticaDeteccionFraude`.

**Dependency Inversion Principle (DIP)**

Los casos de uso dependen de interfaces y no de implementaciones concretas. Las dependencias se suministran mediante inyección por constructor.

Entre los principales contratos se encuentran:

- `TransferCommandPublisher`
- `PublicadorEventosLedger`
- `TransaccionRepository`
- `CuentaRepository`
- `RepositorioAlertasFraude`
- `DifusorDashboard`

**Value Objects**

Se utilizan `record` de Java para representar objetos de valor inmutables:

- `TransferCommandRecord`
- `TransactionEventRecord`
- `FraudAlertRecord`
- `EmailCommandRecord`
- `MontoDinero`
- `TransactionId`

Sus constructores compactos realizan validaciones para evitar montos menores o iguales a cero, divisas no admitidas e identificadores vacíos.

**Persistencia**

Las entidades de persistencia se mantienen separadas del modelo de dominio. Entre ellas se encuentran:

- `TransaccionJpaEntity`
- `CuentaJpaEntity`
- `AlertaFraudeJpaEntity`

Los adaptadores se encargan de convertir la información entre ambos modelos.

Las tablas del ledger y de alertas utilizan `@Immutable` para mantener un esquema de inserción sin modificaciones. Por su parte, las cuentas utilizan bloqueo optimista mediante `@Version`.

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 17 | Lenguaje de programación |
| Spring Boot 3.2 | Desarrollo de microservicios |
| Apache Kafka | Distribución de eventos |
| RabbitMQ | Gestión de comandos y colas |
| PostgreSQL | Persistencia de información |
| Spring Data JPA | Acceso a datos |
| WebSocket | Comunicación en tiempo real |
| Docker | Contenedorización |
| Docker Compose | Orquestación local |
| JUnit 5 | Pruebas unitarias |
| Mockito | Simulación de dependencias |

## Instalación y ejecución

### Requisitos previos

- Docker Desktop.
- Docker Compose v2.

### 1. Iniciar el proyecto

Desde la raíz del repositorio, ejecuta:

```bash
docker compose up -d --build
```

Este comando construye las imágenes e inicia los contenedores definidos en `docker-compose.yml`.

### 2. Realizar una recarga

Recarga $200 a la cuenta A:

```bash
curl -X POST http://localhost:8080/deposit \
-H "Content-Type: application/json" \
-d '{"user":"A","amount":200}'
```

### 3. Realizar una transferencia

Envía $50 desde la cuenta A hacia la cuenta B:

```bash
curl -X POST http://localhost:8080/transfer \
-H "Content-Type: application/json" \
-d '{"from_user":"A","to_user":"B","amount":50}'
```

### 4. Comprobar el rechazo por fondos insuficientes

Intenta enviar $500 desde la cuenta B hacia la cuenta A:

```bash
curl -X POST http://localhost:8080/transfer \
-H "Content-Type: application/json" \
-d '{"from_user":"B","to_user":"A","amount":500}'
```

Como la cuenta B no dispone de saldo suficiente, la operación debe quedar registrada como rechazada.

### 5. Abrir el dashboard

Abre el siguiente archivo en el navegador:

```text
dashboard/dashboard.html
```

Desde allí podrás consultar las cuentas, realizar operaciones y visualizar los movimientos.

### 6. Consultar los trabajadores de correo

Para observar cómo se distribuyen los mensajes entre las tres réplicas de `email_worker`:

```bash
docker compose logs -f email_worker
```

### 7. Consultar la información en PostgreSQL

Para revisar los movimientos, estados, cuentas y alertas de fraude:

```bash
docker exec -it postgres-db psql -U ledger -d ledger -c "SELECT * FROM ledger_movimientos; SELECT * FROM historial_estados; SELECT * FROM cuentas_bancarias; SELECT * FROM alertas_fraude;"
```

### 8. Acceder a RabbitMQ

La consola de administración está disponible en:

`http://localhost:15672`

Credenciales:

- Usuario: `ledger`
- Contraseña: `ledger`

### Consideraciones sobre los datos

Si vienes de una versión anterior del proyecto, puedes reiniciar los contenedores y los volúmenes con:

```bash
docker compose down -v
```

Este procedimiento elimina los datos persistidos. Se recomienda utilizarlo únicamente cuando sea necesario reiniciar el entorno.

Las cuentas actualmente se inicializan con saldo de $0. Las versiones anteriores utilizaban un saldo inicial de $100.000 y una estructura diferente para el ledger.

Al iniciar, `dashboard_aggregator` reconstruye su vista en memoria reproduciendo los eventos de los topics de Kafka desde el inicio.

## Ejecución desde IntelliJ IDEA

También es posible ejecutar y depurar los microservicios individualmente desde IntelliJ IDEA.

### Configuración inicial

1. Abre IntelliJ IDEA y selecciona **File → Open**.
2. Busca el `pom.xml` ubicado en la raíz del proyecto.
3. Selecciona **Open as Project**.
4. Configura el SDK de Java 17.

### Iniciar la infraestructura

Antes de ejecutar los microservicios, inicia los servicios de infraestructura:

```bash
docker compose -f docker-compose.infra.yml up -d
```

También puedes utilizar la configuración `Infraestructura (Docker)` disponible en IntelliJ IDEA.

### Ejecutar los microservicios

En el selector de configuraciones, elige `Ledger - todos los servicios` y pulsa el botón de ejecución.

Si necesitas depurar un componente, utiliza el modo Debug o selecciona la configuración individual del microservicio correspondiente.

Finalmente, abre `dashboard/dashboard.html` en el navegador.

### Configuración de conexiones

Las configuraciones de ejecución se encuentran en `.run/` y son detectadas automáticamente por IntelliJ IDEA.

Los servicios utilizan los siguientes valores predeterminados:

| Servicio | Dirección | Credenciales |
|---|---|---|
| PostgreSQL | `localhost:5433` | `ledger / ledger` |
| Kafka | `localhost:9092` | — |
| RabbitMQ | `localhost:5672` | `ledger / ledger` |

**Importante:** PostgreSQL utiliza el puerto 5433 para evitar conflictos con instalaciones locales de PostgreSQL en Windows.

No ejecutes simultáneamente `docker-compose.infra.yml` y `docker-compose.yml`, ya que ambos utilizan los mismos puertos.

## Pruebas unitarias

El proyecto utiliza **JUnit 5 y Mockito** para verificar el comportamiento de sus componentes.

Las pruebas siguen el patrón AAA (Arrange, Act, Assert) y se ejecutan de forma aislada, sin necesidad de iniciar Spring Boot, PostgreSQL o los brokers de mensajería.

Para ejecutar todas las pruebas sin instalar Maven localmente:

```bash
docker run --rm \
-v "$(pwd)":/app \
-w /app \
maven:3.9.6-eclipse-temurin-17-alpine \
mvn -B test
```

Si tienes Maven instalado, puedes ejecutarlas directamente desde la raíz del proyecto:

```bash
mvn test
```

El `pom.xml` principal incluye los siete módulos, por lo que el comando ejecuta las pruebas de todos los microservicios.

## Configuración de Docker

Cada microservicio cuenta con un Dockerfile multietapa para separar la compilación de la ejecución.

### Construcción de imágenes

**Etapa de compilación**

Utiliza `maven:3.9.6-eclipse-temurin-17-alpine`. Se ejecuta `dependency:go-offline` antes de copiar el código fuente para aprovechar la caché de dependencias.

**Etapa de ejecución**

Utiliza `eclipse-temurin:17-jre-alpine` y ejecuta la aplicación con el usuario no privilegiado `appuser:appgroup`.

También se configuran los siguientes parámetros de la JVM:

```text
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:+ExitOnOutOfMemoryError
```

### Docker Compose

La configuración de contenedores incluye:

- Límites de CPU y memoria mediante `deploy.resources.limits`.
- Tres réplicas para `email_worker`.
- Volumen persistente `pgdata`.
- Healthchecks para PostgreSQL, Kafka, Zookeeper y RabbitMQ.

La infraestructura se distribuye en tres redes:

| Red | Propósito |
|---|---|
| `public-net` | Red con los puertos publicados hacia el host. |
| `messaging-net` | Comunicación interna entre Kafka, Zookeeper y RabbitMQ. |
| `persistence-net` | Comunicación de PostgreSQL con los servicios que requieren persistencia. |

## Decisiones de implementación

Durante el desarrollo se tomaron algunas decisiones para adaptar los requisitos del proyecto a la estructura de los microservicios.

**Generación de publicidad**

Aunque los objetivos de aprendizaje plantean que la publicidad reacciona a los eventos del ledger, la especificación de `ad_generator` establece que debe ejecutarse mediante una tarea programada.

Por este motivo, se implementó utilizando `@Scheduled`, con una ejecución cada 30 segundos. Los mensajes generados se envían a `email_queue`, compartida con `notification_router`.

El procesamiento de eventos de Kafka queda a cargo de `dashboard_aggregator`, `fraud_detector` y `notification_router`.

**Notificaciones de transferencias**

Los correos correspondientes a transferencias completadas utilizan el formato establecido en el enunciado, por ejemplo: `Envió $50 a B`.

Para las recargas y los intentos de transferencia rechazados, se incluyó información sobre el saldo disponible, ya que estos formatos no estaban definidos en la especificación original.

---

**Nota:** Los nombres de servicios, colas, topics, clases y configuraciones corresponden a los utilizados en la implementación del proyecto.
