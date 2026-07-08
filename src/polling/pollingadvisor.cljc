(ns polling.pollingadvisor
  "PollOps-LLM client -- the *contained intelligence node* for the
  polling actor (README: \"PollOps-LLM\").

  It normalizes survey-intake, drafts a per-jurisdiction market-
  research-professional-standards evidence checklist, screens surveys
  for an unresolved unrepresentative-sample risk, and drafts the
  findings-report-publication action. CRITICAL: it is a smart-but-
  untrusted advisor. It returns a *proposal* (with a rationale + the
  fields it cited), never a committed record or a real findings-report
  publication. Every output is censored downstream by `polling.
  governor` before anything touches the SSoT, and `:actuation/
  publish-findings-report` proposals NEVER auto-commit at any phase --
  see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/publish-findings-report | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [polling.facts :as facts]
            [polling.registry :as registry]
            [polling.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the survey, jurisdiction or sample size. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "調査記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :survey/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-methodology
  "Per-jurisdiction market-research-professional-standards evidence
  checklist draft. `:no-spec?` injects the failure mode we must defend
  against: proposing a checklist for a jurisdiction with NO official
  spec-basis in `polling.facts` -- the Survey Integrity Governor must
  reject this (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [sv (store/survey db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction sv))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "polling.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :methodology/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :methodology/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-unrepresentative-sample-risk
  "Unrepresentative-sample-risk screening draft.
  `:unrepresentative-sample-risk-unresolved?` on the survey record
  injects the failure mode: the Survey Integrity Governor must HOLD,
  un-overridably, on any unresolved risk."
  [db {:keys [subject]}]
  (let [sv (store/survey db subject)]
    (cond
      (nil? sv)
      {:summary "対象調査記録が見つかりません" :rationale "no survey record"
       :cites [] :effect :risk-screen/set :value {:survey-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:unrepresentative-sample-risk-unresolved? sv))
      {:summary    (str (:client-name sv) ": 未解決の非代表性サンプルリスクを検出")
       :rationale  "スクリーニングが未解決の非代表性サンプルリスクを検出。人手確認とホールドが必須。"
       :cites      [:sample-representativeness-check]
       :effect     :risk-screen/set
       :value      {:survey-id subject :verdict :unresolved}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:client-name sv) ": 未解決の非代表性サンプルリスクなし")
       :rationale  "非代表性サンプルリスクスクリーニング完了。"
       :cites      [:sample-representativeness-check]
       :effect     :risk-screen/set
       :value      {:survey-id subject :verdict :resolved}
       :stake      nil
       :confidence 0.9})))

(defn- propose-findings-report-publication
  "Draft the actual FINDINGS-REPORT action -- publishing a real
  findings report to a client or the public. ALWAYS `:stake
  :actuation/publish-findings-report` -- this is a REAL-WORLD market-
  research act, never a draft the actor may auto-run. See README
  `Actuation`: no phase ever adds this op to a phase's `:auto` set
  (`polling.phase`); the governor also always escalates on
  `:actuation/publish-findings-report`. Two independent layers agree,
  deliberately."
  [db {:keys [subject]}]
  (let [sv (store/survey db subject)]
    {:summary    (str subject " 向け報告書公開提案"
                      (when sv (str " (client=" (:client-name sv) ")")))
     :rationale  (if sv
                   (str "actual-sample-size=" (:actual-sample-size sv)
                        " minimum-required-sample-size=" (:minimum-required-sample-size sv))
                   "調査記録が見つかりません")
     :cites      (if sv [subject] [])
     :effect     :survey/mark-published
     :value      {:survey-id subject}
     :stake      :actuation/publish-findings-report
     :confidence (if (and sv (not (registry/sample-size-insufficient? sv))) 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :survey/intake                        (normalize-intake db request)
    :methodology/verify                   (verify-methodology db request)
    :risk/screen                          (screen-unrepresentative-sample-risk db request)
    :actuation/publish-findings-report     (propose-findings-report-publication db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは市場調査・世論調査事業の報告書公開エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。説明や前置きは"
       "一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:survey/upsert|:methodology/set|:risk-screen/set|"
       ":survey/mark-published) "
       ":stake(:actuation/publish-findings-report か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :methodology/verify                   {:survey (store/survey st subject)}
    :risk/screen                          {:survey (store/survey st subject)}
    :actuation/publish-findings-report     {:survey (store/survey st subject)}
    {:survey (store/survey st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Survey Integrity Governor
  escalates/holds -- an LLM hiccup can never auto-publish a findings
  report."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :pollingadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
