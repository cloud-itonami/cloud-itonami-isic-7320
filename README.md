# cloud-itonami-isic-7320

Open Business Blueprint for **ISIC Rev.5 7320**: Market research and public opinion polling.

This repository designs a forkable OSS business for market research and public opinion polling -- collecting, analyzing and reporting on data about markets, consumer preferences and public opinion -- run by a qualified, licensed operator so a community or
independent professional never surrenders customer data and ledgers to a
closed SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a fieldwork-kiosk robot collects in-person survey responses where used,
under an actor that proposes actions and an independent **Survey Integrity Governor**
that gates them. The governor never dispatches hardware itself;
`:high`/`:safety-critical` actions require human sign-off.

## Core Contract

```text
intake + identity + engagement records
        |
        v
PollOps-LLM -> Survey Integrity Governor -> hold, proceed, or human approval
        |
        v
engagement ledger + evidence record + audit
```

No automated proposal, by itself, can complete the following without governor
approval and audit evidence: publishing a findings report to a client or the public.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`7320`).

This vertical's engagement records are practice-specific rather than a shared
cross-operator data contract, so it runs on the generic identity/forms/dmn/
bpmn/audit-ledger stack only -- no bespoke domain capability lib.

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Maturity

`:blueprint` -- this repository is the published business/operator design.
The governed actor implementation (`PollOps-LLM` + `Survey Integrity Governor` as
running code) is a follow-up, same as any other `:blueprint`-tier
`cloud-itonami-*` entry in `kotoba-lang/industry`'s registry.

## License

Code and implementation templates are AGPL-3.0-or-later.
