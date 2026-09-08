# Initial capability assessment

The ISIC 7320 repository describes survey intake, evidence assessment, sample-risk
screening and findings reports. ISCO 2431 describes advertising/marketing work and
also a physical print/signage robot. The first cloud mission excludes physical work.

The inspected Itonami resident runtime implements scheduled bounded steps and
checkpoints. Its economy runtime exposes purchaseForBot, but simulation admission
is not payment. The human chat endpoint is a separate runtime and is not automatically
made resident by adding this mission file.

The inspected private funding signer accepts status, launch and start only. It does
not yet expose business spend. Therefore no live USDC purchase or autonomous Bot
startup is asserted by this change.

Next integration requirements: owner-scoped mission loading, resident instance
binding, verified project budget/payees/limits, a business-spend signer adapter,
and one observed task checkpoint with its payment and service receipts if paid.

Sources inspected: ISIC 7320 README and GOVERNANCE; ISCO 2431 README;
network-awai/cloud-itonami workers/grok-bots/itonami_bots_runtime.js and
bot_economy_runtime.js; network-awai/nexus-x402 host/wallet/funding.mjs.
