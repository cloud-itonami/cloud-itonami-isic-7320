(ns polling.registry
  "Pure-function findings-report-publication record construction -- an
  append-only market-research-firm book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a findings-report reference
  number -- every research firm/jurisdiction assigns its own reference
  format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `polling.facts` uses.

  `sample-size-insufficient?` is the SIXTH instance of this fleet's
  MINIMUM-threshold sufficiency check family (`veterinary.registry/
  withdrawal-period-insufficient?`, `funeral.registry/waiting-period-
  elapsed?` and `hospital.registry/observation-period-elapsed?`
  established the first three, all TEMPORAL; `association.registry/
  continuing-education-hours-insufficient?` generalized the family to
  a non-temporal numeric ground truth as the fourth, `secondary.
  registry/attendance-hours-insufficient?` the fifth), applying the
  SAME minimum-floor comparison to a survey's own actual sample size
  against its own recorded minimum-required sample size -- a direct,
  natural mapping onto real statistical-validity practice (a findings
  report published from an undersized sample is exactly the failure
  mode a market-research operator must not let an advisor wave
  through).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real survey-panel/fieldwork system. It builds the RECORD
  a research firm would keep, not the act of publishing the findings
  report itself (that is `polling.operation`'s `:actuation/publish-
  findings-report`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the research firm's own act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn sample-size-insufficient?
  "Does `survey`'s own `:actual-sample-size` fall short of its own
  recorded `:minimum-required-sample-size`? A pure ground-truth check
  against the survey's own permanent fields -- no upstream comparison
  needed. The SIXTH instance of this fleet's MINIMUM-threshold
  sufficiency check family (see ns docstring)."
  [{:keys [actual-sample-size minimum-required-sample-size]}]
  (and (number? actual-sample-size) (number? minimum-required-sample-size)
       (< actual-sample-size minimum-required-sample-size)))

(defn register-findings-report
  "Validate + construct the FINDINGS-REPORT registration DRAFT -- the
  research firm's own act of publishing a real findings report to a
  client or the public. Pure function -- does not touch any real
  survey-panel/fieldwork system; it builds the RECORD a firm would
  keep. `polling.governor` independently re-verifies the survey's own
  sample-size sufficiency and unrepresentative-sample-risk resolution
  status, and blocks a double-publication for the same survey, before
  this is ever allowed to commit."
  [survey-id jurisdiction sequence]
  (when-not (and survey-id (not= survey-id ""))
    (throw (ex-info "findings-report: survey_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "findings-report: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "findings-report: sequence must be >= 0" {})))
  (let [report-number (str (str/upper-case jurisdiction) "-RPT-" (zero-pad sequence 6))
        record {"record_id" report-number
                "kind" "findings-report-draft"
                "survey_id" survey-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "report_number" report-number
     "certificate" (unsigned-certificate "FindingsReport" report-number report-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
