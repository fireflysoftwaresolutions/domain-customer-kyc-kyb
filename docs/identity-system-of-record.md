# Identity system of record

## Ownership and scope

Core KYCB (`core-common-kycb-mgmt`) is the authoritative system of record for KYC/KYB cases, recorded verification outcomes, evidence references, and the separate credential-loss proofing lifecycle. Domain (`domain-customer-kyc-kyb`) orchestrates workflows and integrates third-party verification through adapters. The third party performs the contracted identity or business verification; recording its result does not independently establish that the result is trustworthy.

Native Identity owns credentials, current account eligibility, and credential replacement. It consumes an exact, short-lived Core authorization bound to the customer and recovery ceremony. A KYC/KYB `VERIFIED` status cannot unlock an account or replace credentials. Jafly remains independent: these product responsibilities do not introduce a verifier, trust decision, or recovery policy into the framework.

No verification vendor has been selected. This design does not select or install an identity or biometric engine, define a vendor HTTP API, or provide a production adapter that returns successful proofing. The existing sandbox adapters and successful test fixtures remain synthetic.

## What the current source implements

| Area | Implemented records and controls | Boundary |
| --- | --- | --- |
| KYC/KYB | Core stores party-linked verification status, dates, risk information, review dates, compliance cases, document references, corporate checks, and UBO records. Domain writes workflow transitions through the Core SDK. | CRUD records and workflow outcomes do not prove the external checks occurred or establish an eIDAS assurance level. |
| Representatives | Core `PowerOfAttorney` links `partyId`, `attorneyId`, and a corporate document, with scope, joint-signature conditions, financial limit/currency, effective/expiry dates, and verification/signing flags. Domain signer submission requires an existing `attorneyId`. | Company registration and beneficial ownership do not establish representative authority. Storing a power of attorney or `signingAuthorized` flag is not an implemented mandate trust/revocation check. |
| Recovery cases | Core durably owns case IDs, provider-start reservations, immutable provider-session binding, expiry, bounded failures/grants, authorization consumption, execution reconciliation, and transition audit. Domain owns no recovery database. | Retries reconcile the original reservation and session; they must not create another opportunity after an ambiguous provider response. |
| Recovery evidence | `VerifiedReproofingEvidence` carries case/binding, provider/policy/session IDs, evidence ID/digest, reference UUID, verification/expiry timestamps, and document, live-match, fraud, and reference checks. Core requires all four checks and `referenceId == binding.partyId` before granting, plus fresh Native admission. | The reference equality enforces binding; no live resolver that authenticates the existing party against an authoritative source is supplied. Raw documents and biometrics remain with their designated evidence owner. |
| Authorization | Core binds case, authorization/execution IDs, evidence digest, policy ID, expiry, and the exact issuer/realm/tenant/subject/party/population/purpose/generation/credential-revision/nonce. Current policy permits `HUMAN` and `CREDENTIAL_REPLACEMENT` only. | This cannot confer workforce, administrator, or corporate-representation authority. |

The current `policyId` is an opaque identifier, without a separate policy-version model. `evidenceDigest` is a bounded string; the boundary does not calculate or validate a specified hash algorithm or canonical evidence representation. Successful evidence ID/digest are persisted with the grant, while the full verified-evidence envelope and richer provenance are not stored as a structured assurance record. The existing audit records lifecycle events, not a complete independently validated trust dossier.

Production ports are `ReproofingProviderPort` (`providerId`, `begin`, `verify`), `KybVerificationPort`, and `KybProviderPort`. They are extension points, not completed integrations. Recovery is disabled by default; enabling it without a provider leaves creation and evidence verification unavailable. See [credential-loss proofing adapters](credential-loss-reproofing.md) for the implemented protocol and deployment boundaries.

## eIDAS baseline and required assurance contract

The legal baseline is [Regulation (EU) No 910/2014, consolidated on October 18, 2024](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A02014R0910-20241018), including Article 8's scheme assurance levels, and [Regulation (EU) 2024/1183](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32024R1183), which establishes the European Digital Identity Framework by amending it. These are design references, not evidence of product certification, qualified status, a notified scheme, or an approved bank process.

[Implementing Regulation (EU) 2015/1502](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32015R1502) supplies assurance requirements for identity proofing, means management, authentication, and organizational controls. Its Annex distinguishes natural persons, legal persons, their binding, and renewal/replacement. It requires lifecycle handling for representative bindings. The applicable service policy must determine which requirements and national procedures govern its use case; a documentary or biometric success alone does not establish compliance.

The future vendor contract and structured SoR model must preserve the following information. These are acceptance requirements, **not fields or validators already implemented**:

- Electronic identification scheme, jurisdiction and notification/status evidence; provider-claimed assurance level separately from the relying party's validated trust profile, with validation basis and time.
- Authenticated natural-person and organization identifiers, their mapping to existing party UUIDs, and independently evidenced representative linkage: mandate source, scope, delegation/joint-signature limits, validity, suspension, and revocation.
- Issuer identity and applicable trust validation, evidence provenance, verification method/time, freshness, expiry and revocation/status results. A valid JWT signature proves an authenticated assertion under the configured keys; it does not establish legal qualification or the truth of its claims. A vendor LoA string is a claim requiring validation.
- Immutable policy version and decision basis; retrievable evidence reference, defined hash algorithm/representation, retention/access ownership, and audit correlation. A recovery decision must explicitly cover credential replacement and the original ceremony, rather than reuse a generic onboarding status.

[OpenID Connect for Identity Assurance 1.0](https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html) is a possible representation reference for verified claims and their verification metadata. Identity assurance concerns particular claims and their verification; `acr` describes authentication context. The specification leaves legal/trust-framework rules to the deployment. No OIDC4IDA implementation or eIDAS qualification is claimed here.

## Vendor conformance acceptance

Before production activation, select a provider and approve its trust profile, evidence/retention terms, authoritative-reference policy, assurance mapping, and escalation procedure. Then implement and qualify an actual adapter against the vendor's documented interface and controlled environment.

Acceptance must demonstrate authenticated results and issuer trust, correct existing-party and representative linkage, freshness/revocation behavior, evidence retrieval/hash verification, policy-version traceability, and purpose restrictions. It must also cover original-reservation reconciliation after timeout/restart, duplicate/replayed/swapped sessions, forged/stale results, negative checks, pending/unavailable states, and changed Native eligibility or credentials. Pending results, malformed assertions, and outages remain errors rather than fabricated approvals or identity refusals. Synthetic fixture success is not vendor or bank acceptance.
