(ns polling.facts
  "Per-jurisdiction market-research/survey-professional-standards
  regulatory catalog -- the G2-style spec-basis table the Survey
  Integrity Governor checks every `:methodology/verify` proposal
  against ('did the advisor cite an OFFICIAL public source for this
  jurisdiction's market-research professional-standards and
  respondent-data-protection framework, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official market-
  research professional body and respondent-data-protection law (see
  `:provenance`); they are a STARTING catalog, not a from-scratch
  survey of all ~194 jurisdictions. Extending coverage is additive:
  add one map to `catalog`, cite a real source, done -- never invent a
  jurisdiction's requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  survey-design-record/sampling-methodology-record/fieldwork-record/
  respondent-consent-record evidence set every prior sibling's
  evidence checklist submits in some form; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:actuation/publish-findings-report` proposal
  can commit."
  {"JPN" {:name "Japan"
          :owner-authority "一般社団法人日本マーケティング・リサーチ協会 (Japan Marketing Research Association, JMRA)"
          :legal-basis "個人情報の保護に関する法律 (APPI) / JMRA 「マーケティング・リサーチ綱領」"
          :national-spec "市場調査・世論調査事業者の回答者データ保護および調査品質基準"
          :provenance "https://www.jmra-net.or.jp/rule/kouryou.html"
          :required-evidence ["調査設計記録 (survey-design-record)"
                              "標本抽出方法記録 (sampling-methodology-record)"
                              "実査記録 (fieldwork-record)"
                              "回答者同意記録 (respondent-consent-record)"]}
   "USA" {:name "United States"
          :owner-authority "Insights Association"
          :legal-basis "FTC Act Section 5, 15 U.S.C. § 45 / state consumer-privacy statutes"
          :national-spec "Market-research firm sampling-methodology and respondent-privacy requirements"
          :provenance "https://www.insightsassociation.org/issues-policies/best-practice"
          :required-evidence ["Survey-design record"
                              "Sampling-methodology record"
                              "Fieldwork record"
                              "Respondent-consent record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Market Research Society (MRS)"
          :legal-basis "UK GDPR + Data Protection Act 2018 / MRS Code of Conduct"
          :national-spec "Regulated market-research firm sampling-integrity and respondent-data-protection requirements"
          :provenance "https://www.mrs.org.uk/standards/code-of-conduct"
          :required-evidence ["Survey-design record"
                              "Sampling-methodology record"
                              "Fieldwork record"
                              "Respondent-consent record"]}
   "DEU" {:name "Germany"
          :owner-authority "Berufsverband Deutscher Markt- und Sozialforscher (BVM)"
          :legal-basis "Datenschutz-Grundverordnung (DSGVO) / BVM-Standesregeln"
          :national-spec "Anforderungen an Markt- und Meinungsforschungsinstitute zur Stichprobenintegrität und Befragtendatenschutz"
          :provenance "https://www.bvm.org/standesregeln/"
          :required-evidence ["Erhebungsdesignprotokoll (survey-design-record)"
                              "Stichprobenmethodikprotokoll (sampling-methodology-record)"
                              "Felderhebungsprotokoll (fieldwork-record)"
                              "Befragteneinwilligungsprotokoll (respondent-consent-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to publish a
  findings report on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-7320 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `polling.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
