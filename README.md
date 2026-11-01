<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Customer - KYC and KYB" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Customer - KYC and KYB

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

> Reactive Spring Boot microservice that manages Know Your Customer (KYC) and Know Your Business (KYB) compliance workflows for the Firefly platform.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Module Structure](#module-structure)
- [API Endpoints](#api-endpoints)
- [Domain Logic](#domain-logic)
- [Dependencies](#dependencies)
- [Configuration](#configuration)
- [Running Locally](#running-locally)
- [Testing](#testing)

---

## Overview

`domain-customer-kyc-kyb` provides compliance case-management and workflow orchestration for Firefly customers. It handles two distinct compliance tracks: **KYC** (Know Your Customer) for natural persons and **KYB** (Know Your Business) for legal entities. The service exposes a reactive REST API and internally routes commands and queries through the Firefly CQRS framework (`fireflyframework-cqrs`).

Beneficial ownership is stored as `beneficialOwnershipDocument`. Opening a KYC or KYB case, and attaching KYC evidence, each have a compensating delete on the core. The KYB track provides the primary case-management workflow; documentary, corporate, and UBO verification still require a separately integrated live provider. It orchestrates a five-step distributed saga — `createKybCase → (submitCorporateDocuments ‖ registerUbos) → requestVerification → evaluateResult` — coordinating calls to the downstream `core-common-kycb-mgmt` service via a generated SDK. Every completed saga step publishes a domain event to Kafka, enabling downstream consumers (such as `exp-onboarding` or `core-lending`) to react to compliance outcomes without coupling directly to this service.

The KYC track exposes the lifecycle API (open, attach evidence, mark docs complete, PEP/adverse-media recording, verify, fail, expire, renew) and writes those transitions through the core KYC verification API. Both tracks share a single Spring Boot application, a common infrastructure layer, and the same `KycbClientFactoryImpl` for connecting to the core platform.

---

## Architecture

The service follows a **CQRS + Saga** pattern layered across four runtime modules, with a fifth SDK module published for consumers.

```
HTTP clients
     │
     ▼
┌─────────────────────────────┐
│  -web  (Controllers)        │  KybController, KycController
│  Spring WebFlux / Reactor   │
└────────────┬────────────────┘
             │  CommandBus / QueryBus
             ▼
┌─────────────────────────────┐
│  -core  (Handlers & Saga)   │  Command handlers, Query handlers,
│  fireflyframework-cqrs      │  KybWorkflowSaga (orchestration)
└────────────┬────────────────┘
             │  SDK client beans
             ▼
┌─────────────────────────────┐
│  -infra  (Adapters)         │  KycbClientFactoryImpl, KybVerificationPort,
│  core-common-kycb-mgmt SDK  │  SandboxKybVerificationAdapterImpl
└────────────┬────────────────┘
             │  HTTP (REST)
             ▼
   core-common-kycb-mgmt          (downstream platform service, port 8092)

             │
             ▼
          Kafka                   (topic: domain-layer)
```

The `KybWorkflowSaga` is declared with `@Saga(name = "kyb-workflow", layerConcurrency = 0)` and uses the Firefly orchestration engine to execute steps as a directed acyclic graph (DAG). Steps 2 and 3 (`submitCorporateDocuments` and `registerUbos`) run in parallel within the same topology layer. Step 4 (`requestVerification`) is the join point that waits for both to complete before proceeding to the terminal step 5 (`evaluateResult`).

---

## Module Structure

| Module | Purpose |
|--------|---------|
| `domain-customer-kyc-kyb-interfaces` | Public request/response DTOs shared between the web layer and any consumer of the service's own SDK: `CreateKybCaseRequest`, `SubmitCorporateDocumentsRequest`, `RegisterUbosRequest`, `KybCaseResponse`, `KybResultResponse`. |
| `domain-customer-kyc-kyb-core` | All domain logic: CQRS command/query objects, command handlers, query handlers, the `KybWorkflowSaga`, `KybWorkflowConstants`, `KybWorkflowInput`, and the `KycService` implementation. |
| `domain-customer-kyc-kyb-infra` | Infrastructure adapters: `KycbClientFactoryImpl` (creates reactive API client beans for `core-common-kycb-mgmt`), `KycbClientConfigurationProperties`, the `KybVerificationPort` interface, `SandboxKybVerificationAdapterImpl`, and `KybVerificationResult`. |
| `domain-customer-kyc-kyb-web` | Spring Boot application entry point (`CustomerDomainKycKybApplication`), `KybController`, `KycController`, and `application.yaml`. |
| `domain-customer-kyc-kyb-sdk` | OpenAPI-generated Java client SDK published for other services that need to call this service. Includes `KybApi`, `KycApi`, generated model classes, and a reactive `ApiClient`. |

---

## API Endpoints

### KYB — `/api/v1/kyb`

| Method | Path | Description | Success Status |
|--------|------|-------------|----------------|
| `POST` | `/api/v1/kyb/cases` | Create a new KYB compliance case for a legal entity. Body: `CreateKybCaseRequest` (`partyId`, `businessName`, `registrationNumber`, `tenantId`). Returns `KybCaseResponse` with the generated `caseId`. | `201 Created` |
| `GET` | `/api/v1/kyb/cases/{caseId}` | Retrieve the current state of a KYB compliance case. Returns full `KybCaseResponse` (status, priority, reference, due date, etc.). Result is cached for 5 minutes. | `200 OK` |
| `POST` | `/api/v1/kyb/cases/{caseId}/corporate-documents` | Upload a batch of corporate documents to a KYB case. Body: `SubmitCorporateDocumentsRequest` (`partyId`, `documents[]`). Returns `SubmissionResult` with the list of created document IDs. | `200 OK` |
| `POST` | `/api/v1/kyb/cases/{caseId}/ubos` | Register Ultimate Beneficial Owners for a KYB case. Body: `RegisterUbosRequest` (`partyId`, `ubos[]`). Returns `SubmissionResult` with the list of created UBO IDs. | `200 OK` |
| `POST` | `/api/v1/kyb/cases/{caseId}/verify` | Trigger external KYB verification. Query param: `partyId`. Requires all documents and UBOs to be submitted first. Returns `KybResultResponse`. | `200 OK` |
| `GET` | `/api/v1/kyb/cases/{caseId}/result` | Retrieve the final KYB verification result. Query params: `partyId`, `verificationId`. Returns `KybResultResponse`. Result is cached for 10 minutes. | `200 OK` |

### KYC — `/api/v1/kyc`

| Method | Path | Description | Success Status |
|--------|------|-------------|----------------|
| `POST` | `/api/v1/kyc/cases` | Open a new KYC case with the required due diligence level. | `200 OK` |
| `POST` | `/api/v1/kyc/cases/{caseId}/evidence` | Attach identity, proof-of-address, or selfie documents (fingerprint reference) to a KYC case. Body: `AttachEvidenceCommand`. | `200 OK` |
| `POST` | `/api/v1/kyc/cases/{caseId}/docs-complete` | Mark the document dossier as complete and ready for review. | `200 OK` |
| `POST` | `/api/v1/kyc/cases/{caseId}/pep-adverse-media` | Record PEP (Politically Exposed Person) or adverse media status that will affect the compliance decision. | `200 OK` |
| `POST` | `/api/v1/kyc/cases/{caseId}/verify` | Approve KYC at the required level and set the verification expiry date. | `200 OK` |
| `POST` | `/api/v1/kyc/cases/{caseId}/fail` | Close the KYC case as rejected due to fraud or document inconsistency. | `200 OK` |
| `POST` | `/api/v1/kyc/cases/{caseId}/expire` | Mark a previously verified KYC as expired by time. | `200 OK` |
| `POST` | `/api/v1/kyc/cases/{caseId}/renew` | Renew a KYC verification with a new level and expiry. | `200 OK` |

> **Note:** The KYC endpoints are fully defined and routed but the underlying `KycServiceImpl` currently returns `Mono.empty()` for all operations. The KYC domain logic is under active development.

---

## Domain Logic

### KYB Workflow Saga (`KybWorkflowSaga`)

The saga (`kyb-workflow`) orchestrates the end-to-end KYB compliance process as a DAG with `layerConcurrency = 0`, meaning the orchestration engine controls all concurrency within each topology layer.

**DAG topology:**

```
[Step 1: createKybCase]
      ├── [Step 2: submitCorporateDocuments]  ─┐
      └── [Step 3: registerUbos]              ─┴── [Step 4: requestVerification] ── [Step 5: evaluateResult]
```

Steps 2 and 3 run in parallel. Step 4 is the join point that waits for both. Step 5 is terminal and has no compensation.

**Step details:**

| Step ID | Method | Depends On | Compensation | Timeout | Retries | Event Published |
|---------|--------|------------|-------------|---------|---------|-----------------|
| `createKybCase` | `createKybCase(KybWorkflowInput, ExecutionContext)` | — (root) | `cancelCase` | 30 s | 2 | `kyb.case.created` |
| `submitCorporateDocuments` | `submitCorporateDocuments(ExecutionContext)` | `createKybCase` | `deleteDocuments` | 60 s | 2 | `kyb.documents.submitted` |
| `registerUbos` | `registerUbos(ExecutionContext)` | `createKybCase` | `removeUbos` | 60 s | 2 | `kyb.ubos.registered` |
| `requestVerification` | `requestVerification(ExecutionContext)` | `submitCorporateDocuments`, `registerUbos` | `cancelVerification` | 90 s | 1 | `kyb.verification.requested` |
| `evaluateResult` | `evaluateResult(ExecutionContext)` | `requestVerification` | — (terminal) | 30 s | — | `kyb.verification.completed` |

**`ExecutionContext` key contract** (data written and read across steps):

| Key | Written by | Type | Description |
|-----|-----------|------|-------------|
| `caseId` | Step 1 | `UUID` | Created compliance case ID |
| `partyId` | Step 1 | `UUID` | Owning party ID |
| `documents` | Step 1 | `List<DocumentData>` | Corporate documents to upload |
| `ubos` | Step 1 | `List<UboData>` | UBO records to register |
| `documentIds` | Step 2 | `List<UUID>` | IDs of created corporate documents |
| `uboIds` | Step 3 | `List<UUID>` | IDs of created UBO records |
| `verificationId` | Step 4 | `UUID` | Created KYB verification ID |
| `verificationStatus` | Step 4 | `String` | `VERIFIED` or `REJECTED` |

**Compensation order (strict sequential, reverse):**

```
cancelVerification → removeUbos / deleteDocuments → cancelCase
```

**Step 5 outcome logic:** `evaluateResult` reads `verificationStatus` from context, maps `VERIFIED` to case status `VERIFIED` and any other value to `REJECTED`, then calls `ComplianceCasesApi.updateComplianceCase` to persist the final status before emitting the `kyb.verification.completed` event.

---

### KYB Command Handlers

**`CreateKybCaseHandler`**

Handles `CreateKybCaseCommand`. Calls `ComplianceCasesApi.createComplianceCase` to open a compliance case of type `KYB` with status `OPEN`, priority `MEDIUM`, a 30-day due date, the business name as the case summary, and the registration number as the case reference. Returns the generated `complianceCaseId`. Configured with a 30 s timeout, 2 retries, metrics, and tracing.

**`SubmitCorporateDocumentsHandler`**

Handles `SubmitCorporateDocumentsCommand`. Concurrently uploads each `DocumentData` item via `CorporateDocumentsApi.addCorporateDocument` using `Flux.flatMap` and collects the resulting IDs into a `SubmissionResult`. Configured with a 60 s timeout, 2 retries, metrics, and tracing.

**`RegisterUbosHandler`**

Handles `RegisterUbosCommand`. Concurrently registers each `UboData` item via `UboManagementApi.addUbo` using `Flux.flatMap` and collects the resulting IDs into a `SubmissionResult`. Configured with a 60 s timeout, 2 retries, metrics, and tracing.

**`RequestKybVerificationHandler`**

Handles `RequestKybVerificationCommand`. Delegates to `KybVerificationPort.verify(caseId)` to obtain an external verification result, then persists a `KybVerificationDTO` via `KybVerificationApi.createKybVerification`. Sets the next review date to one year from today and maps the provider outcome to `VERIFIED` or `REJECTED`. Returns the full `KybVerificationDTO` so the saga step can store the verification status and ID in context. Configured with a 60 s timeout, 1 retry, metrics, and tracing.

---

### KYB Query Handlers

**`GetKybCaseHandler`**

Handles `GetKybCaseQuery`. Calls `ComplianceCasesApi.getComplianceCase(caseId)` and returns a `ComplianceCaseDTO`. Results are cached for 5 minutes (`cacheTtl = 300`).

**`GetKybResultHandler`**

Handles `GetKybResultQuery`. Calls `KybVerificationApi.getKybVerification(partyId, verificationId)` and returns a `KybVerificationDTO`. Results are cached for 10 minutes (`cacheTtl = 600`) because verification outcomes are immutable once the `evaluateResult` step completes.

---

### KYB Verification Port

`KybVerificationPort` is the port for an external KYB verification provider. Its sample implementation, `SandboxKybVerificationAdapterImpl`, returns synthetic `VERIFIED` results and activates only when `integration.kyb-verification.provider=sandbox` is explicitly configured. Missing, blank, or unsupported selections do not activate this adapter. With no actual `KybVerificationPort` implementation, the required verification handler refuses startup; configuration absence is not a business `REJECTED` result.

The separate `KybProviderPort` sample, `SandboxKybProviderAdapterImpl`, also requires explicit `kyb.provider=sandbox`. Its business, corporate-document, and UBO successes are synthetic. Neither adapter is a live verification provider. This service supplies no live verifier, identity reproofing, or account-recovery grant. Default, `dev`, `pre`, and `pro` configuration selects neither sample. Intentional sandbox use must explicitly configure both selectors, as shown below.

---

## Dependencies

### Upstream — services this microservice consumes

| Service | Client | APIs Used |
|---------|--------|-----------|
| `core-common-kycb-mgmt` | `KycbClientFactoryImpl` wrapping the `core-common-kycb-sdk` `ApiClient` | `ComplianceCasesApi`, `CorporateDocumentsApi`, `UboManagementApi`, `KybVerificationApi`, `KycVerificationApi`, `KycVerificationDocumentsApi`, `VerificationDocumentsApi` |
| External KYB provider | `KybVerificationPort` (explicit provider required) | Register another adapter for a live provider |
| Kafka | Firefly EDA (publisher-type: KAFKA) | Topic `domain-layer` — events emitted by `@StepEvent` on each saga step |

### Downstream — services that consume this microservice

Downstream services use the `domain-customer-kyc-kyb-sdk` module to call this service over HTTP. Kafka consumers subscribed to the `domain-layer` topic can react to the following events without a direct HTTP dependency:

| Event | Trigger |
|-------|---------|
| `kyb.case.created` | A new KYB compliance case was opened |
| `kyb.documents.submitted` | Corporate documents were uploaded to a case |
| `kyb.ubos.registered` | UBOs were registered against a case |
| `kyb.verification.requested` | An external verification was triggered |
| `kyb.verification.completed` | The verification outcome was recorded and the case status updated |

Noted candidate downstream consumers include `exp-onboarding` and `core-lending`.

---

## Configuration

All properties are defined in `domain-customer-kyc-kyb-web/src/main/resources/application.yaml`.

### Server

| Property | Default | Description |
|----------|---------|-------------|
| `server.address` | `localhost` (env: `SERVER_ADDRESS`) | Bind address |
| `server.port` | `8081` (env: `SERVER_PORT`) | HTTP listen port |
| `server.shutdown` | `graceful` | Graceful shutdown mode |
| `spring.threads.virtual.enabled` | `true` | Enables Java virtual threads |

### Firefly CQRS

| Property | Value | Description |
|----------|-------|-------------|
| `firefly.cqrs.enabled` | `true` | Activates the CQRS framework |
| `firefly.cqrs.command.timeout` | `30s` | Default command bus timeout |
| `firefly.cqrs.command.metrics-enabled` | `true` | Micrometer metrics for commands |
| `firefly.cqrs.command.tracing-enabled` | `true` | Distributed tracing for commands |
| `firefly.cqrs.query.timeout` | `15s` | Default query bus timeout |
| `firefly.cqrs.query.caching-enabled` | `true` | Query result caching |
| `firefly.cqrs.query.cache-ttl` | `15m` | Default query cache TTL |
| `firefly.saga.performance.enabled` | `true` | Enables saga performance metrics |
| `firefly.eda.enabled` | `true` | Enables event-driven architecture support |
| `firefly.eda.default-publisher-type` | `KAFKA` | Default event publisher |
| `firefly.eda.publishers.kafka.default.default-topic` | `domain-layer` | Kafka topic for domain events |
| `firefly.eda.publishers.kafka.default.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `firefly.stepevents.enabled` | `true` | Enables `@StepEvent` publishing on saga steps |

### Downstream client — `core-common-kycb-mgmt`

| Property | Default | Description |
|----------|---------|-------------|
| `api-configuration.core-platform.kycb-mgmt.base-path` | `http://localhost:8092` | Base URL of the kycb-mgmt service |
| `api-configuration.core-platform.kycb-mgmt.timeout` | `10s` | HTTP client timeout |

### OpenAPI / Swagger

| Property | Value | Description |
|----------|-------|-------------|
| `springdoc.api-docs.enabled` | `true` | Enables OpenAPI spec generation |
| `springdoc.api-docs.path` | `/v3/api-docs` | OpenAPI JSON path |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI path |
| `springdoc.packages-to-scan` | `io.fireflysoftware.domain.kyc.kyb.web.controller` | Controllers scanned for API docs |
| `springdoc.paths-to-match` | `/api/**` | Paths included in the OpenAPI spec |

> **Production profile (`pro`):** OpenAPI docs and Swagger UI are disabled. Root log level is `WARN`; `io.fireflysoftware` packages log at `INFO`.

> **Dev and pre profiles:** Both `io.fireflysoftware` and root loggers produce `DEBUG` and `INFO` output respectively.

### Actuator

Exposes the `health`, `info`, and `prometheus` management endpoints. Health details are shown only when authorized. Liveness and readiness state probes are enabled for Kubernetes-style health checks.

---

## Running Locally

**Prerequisites:** Java 25, Maven 3.9+, a running `core-common-kycb-mgmt` instance on port 8092 (or override `api-configuration.core-platform.kycb-mgmt.base-path`), and a Kafka broker on `localhost:9092`.

```bash
mvn clean install -DskipTests
cd /Users/casanchez/Desktop/firefly-oss/domain-customer-kyc-kyb
mvn spring-boot:run -pl domain-customer-kyc-kyb-web
```

Server port: **8081** (default; override with the `SERVER_PORT` environment variable)

The Swagger UI is available at `http://localhost:8081/swagger-ui.html` and the OpenAPI spec at `http://localhost:8081/v3/api-docs`.

To activate the `dev` profile for `DEBUG`-level logging:

```bash
mvn spring-boot:run -pl domain-customer-kyc-kyb-web -Dspring-boot.run.profiles=dev
```

To use a live KYB verifier, register an actual `KybVerificationPort` adapter and explicitly configure its provider selection. Setting a name alone does not create an integration:

```bash
-Dintegration.kyb-verification.provider=<provider-name>
```

For isolated synthetic development only, explicitly select both sample ports:

```bash
-Dintegration.kyb-verification.provider=sandbox -Dkyb.provider=sandbox
```

Do not select these samples for live verification. An unavailable provider must remain an operational failure, rather than a fabricated verification outcome.

---

## Testing

```bash
mvn clean verify
```

Provider-selection context tests also run in the core and infra modules. They cover absent, blank, unsupported, and explicit sandbox selection, stable bean IDs, and startup refusal for a required provider consumer. The provider-error regression checks that no verification is persisted.

The nonpublishing `KYB explicit provider source qualification` PR workflow runs the complete reactor with Java 25. Its temporary relativePath overlay selects source parent `6442bd51733ade4d5f4583e1afe812dd2c520b40` and one Jafly 26.10.23 train. It neither installs a modified released parent nor qualifies live-provider acceptance.

Unit tests are located under `domain-customer-kyc-kyb-core/src/test/java`:

| Test class | Coverage |
|------------|---------|
| `CreateKybCaseHandlerTest` | `CreateKybCaseHandler` — case creation and error paths |
| `SubmitCorporateDocumentsHandlerTest` | `SubmitCorporateDocumentsHandler` — concurrent document upload |
| `RegisterUbosHandlerTest` | `RegisterUbosHandler` — concurrent UBO registration |
| `RequestKybVerificationHandlerTest` | `RequestKybVerificationHandler` — provider delegation and result persistence |
| `GetKybCaseHandlerTest` | `GetKybCaseHandler` — case retrieval |
| `GetKybResultHandlerTest` | `GetKybResultHandler` — verification result retrieval |
| `KybWorkflowSagaTest` | `KybWorkflowSaga` — full DAG execution including parallel steps and compensation paths |

Web-layer tests are located under `domain-customer-kyc-kyb-web/src/test/java`:

| Test class | Coverage |
|------------|---------|
| `KybControllerTest` | `KybController` — HTTP request/response mapping for all KYB endpoints |
| `OpenApiGenApplication` | Generates the OpenAPI specification artifact used to build the SDK |
