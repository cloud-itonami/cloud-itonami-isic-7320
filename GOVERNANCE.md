# Governance

`cloud-itonami-7320` is an OSS open-business blueprint for market research and public opinion polling -- collecting, analyzing and reporting on data about markets, consumer preferences and public opinion.
Governance covers both the capability layer and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Survey Integrity Governor remains independent of the advisor.
- hard policy violations (fabricated spec-basis, conflict of interest, incomplete
  records) cannot be overridden by human approval.
- publishing a findings report to a client or the public always escalates to a human -- never automated.
- every hold, approval and delivery path is auditable.
- personal and customer data stay outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit and data-flow review.

Certified operators can lose certification for:

- bypassing the Survey Integrity Governor's policy checks
- mishandling customer data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
