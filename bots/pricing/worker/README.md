# Pricing resident

Dedicated scheduled price-calculation reviewer using authenticated Murakumo only.
It verifies the initial 2 USDC / 70% contribution calculation and persists the
actual served model, usage and result. This is the first resident slice: it does
not yet refresh market observations, measure actual costs, change published
quotes, or enable research sales. Public /status exposes no credentials.

Schedule is six-hourly. Initial scheduled execution succeeded at
2026-09-08T10:40:44Z, finishing at 10:40:50Z with request ID
chatcmpl-t94TkRsytKtq16nzb0yKTlW1rS6p2H1o (132 tokens). Bind
MURAKUMO_API_KEY as a Worker secret. Incomplete, malformed or incorrect model
output cannot become a successful receipt. POST /run requires a separate PRICING_RUN_TOKEN secret; it invokes the same
review as the scheduler. Concurrent invocations are excluded. Murakumo credentials
do not authorize the administrative route. First authenticated production run
completed on 2026-09-08 using qwen3.8-27b-throughput-b70 (132 tokens, approximately
7 seconds). This is not evidence of research fulfillment or customer sales.
