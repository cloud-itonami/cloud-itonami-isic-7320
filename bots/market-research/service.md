# Competitor and pricing research brief

Status: proposed digital service; not accepting paid orders.

## Buyer and deliverable
For a small business choosing how to position one digital service, deliver a
Japanese Markdown brief comparing three to five relevant competitors. Include
source URLs and retrieval dates, advertised price/currency/tax/billing basis,
deliverables, exclusions, target customer and evidence gaps. Conclude with three
positioning options, separating facts from recommendations. A missing price is
unknown, never zero or an invented estimate.

## Order inputs
Buyer supplies a business topic, target region, intended customer and research
question. Reject requests requiring access to private customer data not granted
to the Bot. Check scope and tool availability before quoting or taking payment.
No outreach, interviews, survey representativeness or guaranteed market demand
is included in this desk-research service.

## Quote and fulfillment
Estimate supplier cost plus delivery/rework reserve; apply the operator's margin
policy. No numerical sale price or margin has yet been approved/configured.
Publish an immutable quote with order ID, scope, USDC amount, Base chain,
verified seller destination, expiration and delivery/refund terms. A deposit
into a funding vault is not payment for an order and is not sales revenue.

Move each order through:
scoped -> quoted -> payment-pending -> paid -> researching -> quality-checked
-> delivered -> closed.

Only a canonical successful payment receipt matching the quoted asset, amount,
chain, destination and order reference can mark paid. A tx hash or browser
callback alone is insufficient. Replayed receipts cannot pay another order.
Do not charge again after a timeout: reconcile the same order/payment ID.

Save incremental research in the owner-scoped workspace. Quality checks require
source-backed prices, clear unknowns and no unsupported demand claims. Release
the report through the buyer's authenticated download only after confirmed
payment and a passing quality check. Retain version/hash and delivery receipt.
If the task fails after payment, retain the order and follow its disclosed refund
terms; do not report a delivered product or silently lose the order.

## Operating decision
No routine human approval is needed for research, quality checks, or a supplier
purchase within a verified delegated grant. Budget, recipient and expiry checks
are enforced by the signer/contract, independently of model text. Yield-only
funding never spends principal. Stop purchasing on uncertain payment outcomes.

## Revenue evidence
Record order ID, received USDC, supplier expense, fees, delivery receipt and
refunds separately. Track net contribution (sales less costs/refunds), not merely
Bot activity or deposits. Positive revenue or profit is not yet observed.

## Remaining live connections
- A market-research resident instance that loads SOUL.md and completes a checkpoint.
- Supplier spending authority connected to the actual project budget.
- Seller identity and verified USDC receiving route bound to order quotes.
- Order intake, payment verification and authenticated digital delivery.
- A successful paid order with independently checked receipts.
