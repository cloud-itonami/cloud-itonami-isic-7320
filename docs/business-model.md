# Business Model: Market research and public opinion polling

## Classification

- Repository: `cloud-itonami-isic-7320`
- ISIC Rev.5: `7320`
- Activity: market research and public opinion polling -- collecting, analyzing and reporting on data about markets, consumer preferences and public opinion
- Social impact: professional standards, data sovereignty, transparent audit

## Customer

- independent market-research firms
- cooperative polling collectives
- community opinion-research programs

## Offer

- survey-design intake
- fieldwork/sampling proposal
- findings-report proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per firm
- support: monthly retainer with SLA
- migration: import from an incumbent survey-panel system
- per-survey fee

## Trust Controls

- no findings report is published without human sign-off
- a fabricated or unrepresentative sample forces a hold, not an override
- every publication path is auditable
- emergency manual override paths remain outside LLM control
- a fabricated jurisdiction citation, incomplete evidence, or a sample size
  below its own recorded minimum-required size -- each forces a hold, not an
  override
- findings-report publication is logged and escalated, and cannot be
  finalized twice for the same survey: a double-publication attempt is held
  off this actor's own survey facts alone, with no upstream comparison
  needed

## Survey Integrity Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:survey-
integrity-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (publishing a
findings report to a client or the public) must pass. The governor
sits between the PollOps-LLM and execution, per the README's Core
Contract:

```text
PollOps-LLM -> Survey Integrity Governor -> hold, proceed, or human approval
```

**Approves**: routine market-research actions proposed against a
survey that already has a consented methodology on file, a sample
size meeting its own recorded minimum requirement, and no unresolved
unrepresentative-sample risk. These proceed straight to the
engagement ledger.

**Rejects or escalates**: the governor refuses to let the advisor
publish a findings report on its own authority when any of the
following hold -- a fabricated jurisdiction spec-basis; incomplete
evidence; a sample size below its own minimum-required size; an
unresolved unrepresentative-sample risk. A clean publication proposal
still always routes to a human -- `:actuation/publish-findings-
report` is never auto-committed, at any rollout phase.
