# First funding round — preparation only

The candidate is Itonami Market Research Bot, attached to this existing public project. SOUL.md defines a concrete service: scoped, source-backed research reports. This repository does not itself create a human-owned Bot instance, bind credentials, publish borrowing terms or deploy a vault.

The proposed pilot is **100 USDC**, with a **10 USDC daily spending limit**, **20 USDC cash reserve**, seven days to collect funding and maturity thirty days after collection closes. These are editable draft numbers, not approved terms. No funds have been requested or moved.

Before publication the owner must name the legally responsible borrower, its repayment obligation, controller wallet and genuine approved USDC payees, and accept the final dates and amounts. Kotoba Labs Inc supplies infrastructure; that fact does not make it the borrower. Do not substitute an operator wallet for a vendor without explicit authorization.

The current fixed-round implementation distributes actual settlement assets pro rata to lenders. It has no guaranteed APR. It recognizes unpaid debt after the seven-day maturity grace period, and settlement can depend on recovering Aave liquidity. Contract behavior does not by itself define a borrower's legal obligation. Final public conditions must accurately describe both.

## Activation sequence

1. Sign into app.itonami.cloud with the intended owner wallet and connect a GitHub account with administrator access to this public repository.
2. Create the owner-scoped Bot using SOUL.md, connect the actual research tools and run a no-spend sourced report to validate the runtime.
3. Open the project's funding page, register the agreed public conditions, and retain the returned immutable version.
4. Create a round with that version, final future deadlines, exact limits and verified payee addresses. Review the wallet deployment request. The owner signs the Base transaction and pays the displayed gas.
5. Wait for the existing receipt confirmation and deployed-code verification. Check that the public directory reports accepting funding from the live phase, deadline and cap. Do not set the display flag manually.
6. Customer receipts, Bot spending, DeFi allocation and lender settlement each need their own actual evidence. A successful vault deployment does not prove those operations.

Funding page: https://app.itonami.cloud/ja/#capital=cloud-itonami%2Fcloud-itonami-isic-7320

## Owner direction: two separate models

The owner named **Awai network** as the responsible organization; its registered legal identity and jurisdiction still need confirmation. Preserve the principal-spending business-loan draft and offer a separate `yield-budget-v1` round. Each needs its own explicit terms/version and vault. The yield round's `botShareBps` remains unset: the implementation permits an explicit split, but no particular share was authorized. Neither draft is accepting deposits. The business profile may serve both, while accounts and authority stay round-specific.
