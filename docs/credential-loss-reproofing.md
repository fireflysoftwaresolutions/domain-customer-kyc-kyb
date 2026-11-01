# Customer credential-loss proofing adapters

This candidate is independent from the existing KYB sandbox and known-password SCA unlock protocol. It has no database. Core KYCB owns durable cases, provider-start reservations, immutable provider sessions, authorization expiry, failure budgets and execution audit. Native Identity owns local credentials and current customer eligibility. Jafly is unchanged.

## Production boundary

The feature is off unless `firefly.reproofing.enabled=true`. Both `/api/v1/identity-recovery/cases/**` and `/internal/identity-recovery/**` remain denied when it is off. Enabling the feature without a `ReproofingProviderPort` leaves case creation and evidence verification unavailable. No production adapter produces successful documentary or biometric evidence.

A real adapter must authenticate the actual provider result server to server and positively establish the existing party UUID through an approved authoritative reference. It must implement durable, idempotent begin/reconciliation using Core's exact `providerStartId`; an ambiguous response cannot create a fresh provider session or opportunity. Domain retries never create another key. The adapter must bind exact case, provider session, nonce, issuer, realm, tenant, subject, generation and full credential revision; validate document authenticity, live capture matching, fraud and authoritative-reference checks; and retain raw documents/biometrics with their proper evidence owner. There is no public callback or arbitrary approval endpoint. Names and email addresses never establish a link.

The customer policy accepts one exact configured issuer, realm and tenant, population `HUMAN` and purpose `CREDENTIAL_REPLACEMENT`. STAFF, other issuers, substituted sessions, references, expired evidence and changed bindings fail closed. Core independently invokes the private Domain verifier and Native admission; native callers cannot invoke evidence verification. Post-commit execution recording relies on Core's authenticated native journal, including audit reconciliation after expiry, and cannot renew authorization.

## Explicit configuration

All properties below use prefix `firefly.reproofing.`:

| Property | Purpose |
| --- | --- |
| `issuer`, `realm`, `tenant` | Exact customer authority |
| `provider-id`, `policy-id` | Approved configured provider and reference policy |
| `verification-origin` | Exact HTTPS provider journey origin, without a path |
| `native-client`, `native-secret-file` | Inbound native orchestration identity |
| `verifier-client`, `verifier-secret-file` | Separate inbound Core evidence verifier |
| `core-origin`, `core-client`, `core-secret-file` | Original Core producer HTTPS origin and Domain workload |
| `native-base-url` | Exact `issuer + /firefly-recovery/lost-credentials/rpc` |
| `native-rpc-client`, `native-rpc-secret-file` | Separate grant-disabled Domain native admission client |

Secrets are explicit mounted files containing 32–512 characters. Native and Core verifier identities must differ. HTTP Basic is accepted only on HTTPS; outbound TLS uses the JVM trust store, never trust-all, and redirects are disabled. Install the approved CA in the normal trust store. Requests/responses are bounded and remote calls time out. Only exact POST actions are allowed. Recovery bodies are limited to 32 KiB and five seconds before dispatch. The configured generic idempotency header is stripped only on recovery paths, and original-producer operations disable generic response reuse; Core remains the durable retry authority. Provider-session binding is internal to Domain/Core and is not exposed to native clients.

## API and retries

`POST /api/v1/identity-recovery/cases` returns a `ReproofingJourney` with JSON fields `case` and nullable `verificationUrl`. The URL must match the exact configured HTTPS origin. Inspect and confirm return the Core case; consume and verify return the exact authorization; execution recording returns the `EXECUTED` case. Private authorization IDs never enter the browser. The Core reservation, case ID, provider session and original expiry survive retries unchanged. Domain uses the genuinely generated `IdentityReproofingApi` from the original Core producer, with independent Domain wire records and UTC timestamp codecs.

## Hosted qualification and limits

`tooling/prepare_reproofing_sdk_fixture.py` runs only on the owned hosted runner. It pins the Core source in `tooling/reproofing-core-source.txt`, source parent `6442bd51733ade4d5f4583e1afe812dd2c520b40`, and Jafly 26.10.23. It builds the original producer OpenAPI and SDK under a distinct candidate SDK version, flattens only that candidate descriptor, and verifies the complete Domain reactor. It never installs a modified released parent GAV or edits generated SDK sources. Its summary records source pins, original descriptor hashes, producer specification hash and candidate SDK hash.

The successful provider and control API exist only in `src/test/java` under `ReproofingFixtureApplication`. The fixture persists signed sessions in its private attempt-owned directory and reconciles the same Core reservation after a provider restart. The authenticated test-only control endpoint is `POST /fixture/reproofing/sessions/{providerSessionId}` with `{ "mode": "APPROVED" }`; negative modes are `FORGED`, `EXPIRED`, `WRONG_REFERENCE`, `REPLAYED` and `UNAVAILABLE`. A separate mounted test identity authorizes this endpoint. Production JAR scans refuse any packaged fixture class.

These mechanisms and synthetic fixtures do not establish real-provider assurance or production activation. Actual provider approval, biometric/document/reference policy and licenses/thresholds, bank escalation procedure, notification delivery acceptance, receiving-service live enforcement of retired JWTs and retained-database rollout remain separate external gates. The customer owner never restores workforce or administrator authority.

The optional framework resource-server module is exercised only as a published test dependency here. If an installation includes that module and enables its policy, startup requires a separate existing `SecurityWebFilterChain` named `fireflySecurityWebFilterChain` (or the explicit `existing-security-chain-bean` property). The product refuses startup if adding its narrow chain would suppress that application policy. It does not reconstruct generic JWT policy or supply a global permit chain. The synthetic runtime explicitly disables the optional generic module and qualifies only these product boundaries; unrelated CRUD authentication remains a separate qualification.
