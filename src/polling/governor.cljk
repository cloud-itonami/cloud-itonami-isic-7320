(ns polling.governor
  "Survey Integrity Governor -- the independent compliance layer that
  earns the PollOps-LLM the right to commit. The LLM has no notion of
  market-research professional-standards law, whether a survey's own
  actual sample size actually reaches its own recorded minimum
  requirement, whether an unrepresentative-sample risk against a
  survey has actually stayed unresolved, or when an act stops being a
  draft and becomes a real-world findings-report publication, so this
  MUST be a separate system able to *reject* a proposal and fall back
  to HOLD -- the market-research analog of `cloud-itonami-isic-6512`'s
  CasualtyGovernor.

  Four checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence, an
  undersized sample, or an unresolved unrepresentative-sample risk).
  The confidence/actuation gate is SOFT: it asks a human to look (low
  confidence / actuation), and the human may approve -- but see
  `polling.phase`: for `:stake :actuation/publish-findings-report` (a
  real client/public-facing act) NO phase ever allows auto-commit
  either. Two independent layers agree that actuation is always a
  human call.

    1. Spec-basis                  -- did the methodology proposal
                                       cite an OFFICIAL source
                                       (`polling.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:actuation/publish-
                                       findings-report`, has the
                                       survey actually been assessed
                                       with a full survey-design-
                                       record/sampling-methodology-
                                       record/fieldwork-record/
                                       respondent-consent-record
                                       evidence checklist on file?
    3. Sample size insufficient    -- for `:actuation/publish-
                                       findings-report`, INDEPENDENTLY
                                       recompute whether the survey's
                                       own actual sample size falls
                                       short of its own recorded
                                       minimum-required sample size
                                       (`polling.registry/sample-size-
                                       insufficient?`) -- needs no
                                       proposal inspection at all. The
                                       SIXTH instance of this fleet's
                                       MINIMUM-threshold sufficiency
                                       check family (`veterinary.
                                       governor/withdrawal-period-
                                       insufficient-violations`/
                                       `funeral.governor/waiting-
                                       period-elapsed-violations`/
                                       `hospital.governor/observation-
                                       period-elapsed-violations`
                                       established the first three,
                                       all TEMPORAL; `association.
                                       governor/continuing-education-
                                       hours-insufficient-violations`
                                       the fourth, non-temporal;
                                       `secondary.governor/attendance-
                                       hours-insufficient-violations`
                                       the fifth).
    4. Unrepresentative-sample risk
       unresolved                     -- reported by THIS proposal
                                       itself (a `:risk/screen` that
                                       just found one), or already on
                                       file for the survey (`:risk/
                                       screen`/`:actuation/publish-
                                       findings-report`). Evaluated
                                       UNCONDITIONALLY (not scoped to
                                       a specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/...
                                       (thirty-eight prior siblings,
                                       most recently `advertising.
                                       governor/misleading-claim-risk-
                                       unresolved-violations`)...
                                       established -- the THIRTY-NINTH
                                       distinct application of this
                                       exact discipline overall, and a
                                       genuinely NEW concept (grep-
                                       verified absent from every prior
                                       sibling's check names before
                                       this claim was finalized),
                                       grounded directly in this
                                       blueprint's own Trust Control 'a
                                       fabricated or unrepresentative
                                       sample forces a hold, not an
                                       override'.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       publish-findings-report` (a REAL
                                       client/public-facing act) ->
                                       escalate.

  One more guard, double-publication prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-published-violations` refuses to
  publish a findings report for the SAME survey twice, off a dedicated
  `:findings-report-published?` fact (never a `:status` value) -- the
  SAME 'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [polling.facts :as facts]
            [polling.registry :as registry]
            [polling.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Publishing a real findings report to a client or the public is the
  ONE real-world actuation event this actor performs -- a single-
  member set, matching `leasing`'s/`underwriting`'s/`testlab`'s/
  `clinic`'s/`veterinary`'s/`funeral`'s/`parksafety`'s/`salon`'s/
  `entertainment`'s/`facility`'s/`consulting`'s/`advertising`'s
  single-actuation shape, grounded directly in this blueprint's own
  README ('No automated proposal, by itself, can complete the
  following without governor approval and audit evidence: publishing
  a findings report to a client or the public')."
  #{:actuation/publish-findings-report})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:methodology/verify` (or `:actuation/publish-findings-report`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's market-research professional-standards
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:methodology/verify :actuation/publish-findings-report} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は市場調査専門職基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/publish-findings-report`, the jurisdiction's
  required survey-design-record/sampling-methodology-record/
  fieldwork-record/respondent-consent-record evidence must actually be
  satisfied -- do not trust the advisor's self-reported confidence
  alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/publish-findings-report)
    (let [sv (store/survey st subject)
          methodology (store/methodology-of st subject)]
      (when-not (and methodology
                     (facts/required-evidence-satisfied?
                      (:jurisdiction sv) (:checklist methodology)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(調査設計記録/標本抽出方法記録/実査記録/回答者同意記録等)が充足していない状態での提案"}]))))

(defn- sample-size-insufficient-violations
  "For `:actuation/publish-findings-report`, INDEPENDENTLY recompute
  whether the survey's own actual sample size falls short of its own
  recorded minimum-required sample size via `polling.registry/sample-
  size-insufficient?` -- needs no proposal inspection at all, since
  its inputs are permanent ground-truth fields already on the
  survey."
  [{:keys [op subject]} st]
  (when (= op :actuation/publish-findings-report)
    (let [sv (store/survey st subject)]
      (when (registry/sample-size-insufficient? sv)
        [{:rule :sample-size-insufficient
          :detail (str subject " の実サンプルサイズ(" (:actual-sample-size sv)
                      ")が必要最小標本数(" (:minimum-required-sample-size sv) ")を下回る")}]))))

(defn- unrepresentative-sample-risk-unresolved-violations
  "An unresolved unrepresentative-sample risk -- reported by THIS
  proposal (e.g. a `:risk/screen` that itself just found one), or
  already on file in the store for the survey (`:risk/screen`/
  `:actuation/publish-findings-report`) -- is a HARD, un-overridable
  hold. Evaluated UNCONDITIONALLY (not scoped to a specific op) so the
  screening op itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        survey-id (when (contains? #{:risk/screen :actuation/publish-findings-report} op) subject)
        hit-on-file? (and survey-id (= :unresolved (:verdict (store/risk-screen-of st survey-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :unrepresentative-sample-risk-unresolved
        :detail "未解決の非代表性サンプルリスクがある調査の報告書公開提案は進められない"}])))

(defn- already-published-violations
  "For `:actuation/publish-findings-report`, refuses to publish a
  findings report for the SAME survey twice, off a dedicated
  `:findings-report-published?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/publish-findings-report)
    (when (store/survey-already-published? st subject)
      [{:rule :already-published
        :detail (str subject " は既に報告書公開済み")}])))

(defn check
  "Censors a PollOps-LLM proposal against the governor rules. Returns
  {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (sample-size-insufficient-violations request st)
                           (unrepresentative-sample-risk-unresolved-violations request proposal st)
                           (already-published-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
