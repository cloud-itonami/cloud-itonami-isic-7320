# ADR-0001: PollOps-LLM ⊣ Survey Integrity Governor architecture

## Status

Accepted. `cloud-itonami-isic-7320` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-7320` publishes an OSS business blueprint for
market research and public opinion polling: collecting, analyzing and
reporting on data about markets, consumer preferences and public
opinion. Like every prior actor in this fleet, the blueprint alone is
not an implementation: this ADR records the governed-actor
architecture that promotes it to real, tested code, following the
same langgraph-clj StateGraph + independent Governor + Phase 0→3
rollout pattern established by `cloud-itonami-isic-6511` (life
insurance) and applied across fifty-four prior siblings, most recently
`cloud-itonami-isic-7310` (advertising).

## Decision

### Decision 1: single-actuation shape

This blueprint's own README, business-model.md and operator-guide.md
consistently name only ONE real-world act: "publishing a findings
report to a client or the public." Matching `leasing`/`underwriting`/
`testlab`/`clinic`/`veterinary`/`funeral`/`parksafety`/`salon`/
`entertainment`/`facility`/`consulting`/`advertising`'s single-
actuation shape, `high-stakes` here is a one-member set,
`#{:actuation/publish-findings-report}`.

### Decision 2: entity and op shape

The primary entity is a `survey`. Four ops: `:survey/intake`
(directory upsert, no capital risk), `:methodology/verify` (per-
jurisdiction market-research-professional-standards evidence
checklist, never auto), `:risk/screen` (unrepresentative-sample-risk
screening, unconditional-evaluation discipline, never auto), and
`:actuation/publish-findings-report` (POSITIVE, high-stakes --
publishing a real findings report to a client or the public).

### Decision 3: `sample-size-insufficient?` -- the 6th MINIMUM-threshold check

Following `veterinary.registry/withdrawal-period-insufficient?` (1st,
temporal), `funeral.registry/waiting-period-elapsed?` (2nd, temporal),
`hospital.registry/observation-period-elapsed?` (3rd, temporal),
`association.registry/continuing-education-hours-insufficient?` (4th,
generalized to non-temporal) and `secondary.registry/attendance-
hours-insufficient?` (5th, non-temporal), `polling.registry/sample-
size-insufficient?` applies the same minimum-floor comparison to a
survey's own actual sample size against its own recorded minimum-
required sample size -- a direct, natural mapping onto real
statistical-validity practice. Gates only `:actuation/publish-
findings-report`.

### Decision 4: `unrepresentative-sample-risk-unresolved-violations` -- the 39th unconditional-evaluation screening grounding, a genuinely new concept

Before writing this check, every prior sibling's governor/registry
namespaces were grepped for `unrepresentative`, `sample-size` and
`representativeness` -- zero hits, confirming this is a genuinely new
concept, avoiding the false-precedent-claim risk `leasing`'s ADR-0001
documents. `unrepresentative-sample-risk-unresolved-violations` reuses
the unconditional-evaluation DISCIPLINE (`casualty.governor/
sanctions-violations`'s original fix) for the 39th distinct
application overall, continuing the count established across this
window's builds (water=25th ... advertising=38th, polling=39th).
Grounded directly in this blueprint's own Trust Control "a fabricated
or unrepresentative sample forces a hold, not an override." Gates
`:risk/screen` and `:actuation/publish-findings-report`.

### Decision 5: dedicated double-actuation-guard boolean

`:findings-report-published?` is a dedicated boolean on the `survey`
record, never a single `:status` value -- the same discipline every
prior sibling governor's guards establish, informed by `cloud-
itonami-isic-6492`'s real status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`polling.store/Store` is implemented by both `MemStore` (atom-backed,
default for dev/tests/demo) and `DatomicStore` (`langchain.db`-
backed), proven to satisfy the same contract in `test/polling/
store_contract_test.clj` -- the same seam every sibling actor uses so
swapping the SSoT backend is a configuration change, not a rewrite.
The protocol's per-entity accessor is named `survey` directly -- not a
Clojure special form, so no `-of` suffix workaround was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:survey/intake` (no
capital risk). `:methodology/verify` and `:risk/screen` are never
auto-eligible at any phase (matching every sibling's screening-op
posture), and `:actuation/publish-findings-report` is permanently
excluded from every phase's `:auto` set -- a structural fact, not a
rollout milestone, enforced by BOTH `polling.phase` and `polling.
governor`'s `high-stakes` set independently.

### Decision 8: no bespoke domain capability lib

This vertical's survey records are practice-specific rather than a
shared cross-operator data contract, so `polling.*` runs on the
generic robotics/identity/forms/dmn/bpmn/audit-ledger stack only --
matching this blueprint's own already-correct `blueprint.edn` (the
ONLY inconsistency found this build was the missing `:maturity` field
itself).

### Decision 9: mock + LLM advisor pair

`polling.pollingadvisor` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
publishing a findings report).

## Alternatives considered

- **A dual-actuation shape** (e.g. adding a separate "release raw
  dataset" actuation alongside findings-report publication).
  Rejected: the blueprint's own text consistently names only ONE
  real-world act; inventing a second would not be grounded in the
  blueprint's own text.
- **A single "survey-integrity" check merging sample-size and
  unrepresentative-sample-risk concerns.** Rejected: sample-size is a
  ground-truth numeric recompute needing no proposal inspection;
  unrepresentative-sample-risk status is an unconditionally-evaluated
  flag that must also HARD-hold the screening op itself on its own
  finding -- merging them would lose the screening op's self-hold
  property.

## Consequences

- Fifty-fifth actor in this fleet (54 implemented before this build).
- Confirms the MINIMUM-threshold sufficiency check family generalizes
  to a sixth instance, genuinely distinct domain (statistical sample
  validity).
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (unrepresentative-sample-risk), grep-verified absent from
  every prior sibling before the claim was finalized.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/polling/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no `:id`/`:required-technologies` fixes
  this time (already correct) -- only the `:maturity` flip itself,
  matching `advertising`'s own experience.
