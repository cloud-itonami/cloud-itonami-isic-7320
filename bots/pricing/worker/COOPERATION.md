# Active research workflow

1. Collector: deterministic HTTPS fetch and price extraction from Exa.
2. Analyst: Murakumo inference computes the budget and qualitative analysis.
3. Quality reviewer: separate Murakumo inference evaluates unsupported claims.
4. Publisher: writes the public artifact to Kotobase and verifies exact readback.

The collector and publisher are code stages, not additional model agents.
This workflow runs every six hours. No automatic agent creation or expansion of
permissions takes place. Work is bounded to one source and two model calls.

GET /catalog exposes at most the latest 100 successful artifacts. Each artifact
has an immutable CID; successful run records remain in the Durable Object under
a timestamp key. Private conversations have not been migrated to Kotobase.

Current entries are FREE PREVIEWS, not paid products. Do not claim purchases,
customer demand or an autonomous economy from internal runs. Paid research orders
remain disabled until production payment/entitlement/delivery is integrated and
verified. The 2 USDC candidate is not a charge for this public one-provider sample.
