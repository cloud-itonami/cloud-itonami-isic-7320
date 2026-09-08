# Pricing resident

Dedicated scheduled price-calculation reviewer using authenticated Murakumo only.
It verifies the initial 2 USDC / 70% contribution calculation and persists the
actual served model, usage and result. This is the first resident slice: it does
not yet refresh market observations, measure actual costs, change published
quotes, or enable research sales. Public /status exposes no credentials.

Schedule is temporarily every five minutes for initial deployment verification;
reduce to six-hourly after the first observed successful scheduled run. Bind
MURAKUMO_API_KEY as a Worker secret. Incomplete, malformed or incorrect model
output cannot become a successful receipt. The external handler cannot trigger
reviews; only scheduled execution invokes the private Durable Object route.
