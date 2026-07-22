(ns polling.store
  "SSoT for the polling actor, behind a `Store` protocol so the
  backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/polling/store_contract_test.clj), which is the whole point:
  the actor, the Survey Integrity Governor and the audit ledger never
  know which SSoT they run on.

  Like `leasing`/`underwriting`/`testlab`/`clinic`/`veterinary`/
  `funeral`/`parksafety`/`salon`/`entertainment`/`facility`/
  `consulting`/`advertising`, this actor has ONE actuation event
  (publishing a real findings report to a client or the public)
  acting on a `survey` entity, with its OWN history collection,
  sequence counter and dedicated double-actuation-guard boolean
  (`:findings-report-published?`, never a `:status` value) -- the
  same discipline every prior sibling governor's guards establish,
  informed by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320).

  The ledger stays append-only on every backend: 'which survey was
  screened for an unresolved unrepresentative-sample risk, which
  findings report was published, on what jurisdictional basis,
  approved by whom' is always a query over an immutable log -- the
  audit trail a client/public trusting a research firm needs, and the
  evidence a firm needs if a publication decision is later disputed."
  (:require [polling.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (survey [s id])
  (all-surveys [s])
  (risk-screen-of [s survey-id] "committed unrepresentative-sample-risk screening verdict for a survey, or nil")
  (methodology-of [s survey-id] "committed methodology evidence assessment, or nil")
  (ledger [s])
  (report-history [s] "the append-only findings-report history (polling.registry drafts)")
  (next-report-sequence [s jurisdiction] "next report-number sequence for a jurisdiction")
  (survey-already-published? [s survey-id] "has this survey's findings report already been published?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-surveys [s surveys] "replace/seed the survey directory (map id->survey)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained survey set covering the actuation lifecycle
  (publishing a findings report) so the actor + tests run offline."
  []
  {:surveys
   {"survey-1" {:id "survey-1" :client-name "Sato Retail Group"
               :actual-sample-size 1200 :minimum-required-sample-size 1000
               :unrepresentative-sample-risk-unresolved? false
               :findings-report-published? false
               :jurisdiction "JPN" :status :intake}
    "survey-2" {:id "survey-2" :client-name "Atlantis Consumer Panel"
               :actual-sample-size 1200 :minimum-required-sample-size 1000
               :unrepresentative-sample-risk-unresolved? false
               :findings-report-published? false
               :jurisdiction "ATL" :status :intake}
    "survey-3" {:id "survey-3" :client-name "鈴木商事アンケート"
               :actual-sample-size 600 :minimum-required-sample-size 1000
               :unrepresentative-sample-risk-unresolved? false
               :findings-report-published? false
               :jurisdiction "JPN" :status :intake}
    "survey-4" {:id "survey-4" :client-name "田中世論調査"
               :actual-sample-size 1200 :minimum-required-sample-size 1000
               :unrepresentative-sample-risk-unresolved? true
               :findings-report-published? false
               :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- publish-findings-report!
  "Backend-agnostic `:survey/mark-published` -- looks up the survey via
  the protocol and drafts the findings-report record, and returns
  {:result .. :survey-patch ..} for the caller to persist."
  [s survey-id]
  (let [sv (survey s survey-id)
        seq-n (next-report-sequence s (:jurisdiction sv))
        result (registry/register-findings-report survey-id (:jurisdiction sv) seq-n)]
    {:result result
     :survey-patch {:findings-report-published? true
                   :report-number (get result "report_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (survey [_ id] (get-in @a [:surveys id]))
  (all-surveys [_] (sort-by :id (vals (:surveys @a))))
  (risk-screen-of [_ id] (get-in @a [:risk-screens id]))
  (methodology-of [_ survey-id] (get-in @a [:methodologies survey-id]))
  (ledger [_] (:ledger @a))
  (report-history [_] (:reports @a))
  (next-report-sequence [_ jurisdiction] (get-in @a [:report-sequences jurisdiction] 0))
  (survey-already-published? [_ survey-id] (boolean (get-in @a [:surveys survey-id :findings-report-published?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :survey/upsert
      (swap! a update-in [:surveys (:id value)] merge value)

      :methodology/set
      (swap! a assoc-in [:methodologies (first path)] payload)

      :risk-screen/set
      (swap! a assoc-in [:risk-screens (first path)] payload)

      :survey/mark-published
      (let [survey-id (first path)
            {:keys [result survey-patch]} (publish-findings-report! s survey-id)
            jurisdiction (:jurisdiction (survey s survey-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:report-sequences jurisdiction] (fnil inc 0))
                       (update-in [:surveys survey-id] merge survey-patch)
                       (update :reports registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-surveys [s surveys] (when (seq surveys) (swap! a assoc :surveys surveys)) s))

(defn seed-db
  "A MemStore seeded with the demo survey set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :methodologies {} :risk-screens {} :ledger [] :report-sequences {}
                           :reports []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (methodology/risk-screen payloads, ledger facts,
  report records) are stored as EDN strings so `langchain.db` doesn't
  expand them into sub-entities -- the same convention every sibling
  actor's store uses."
  {:survey/id                          {:db/unique :db.unique/identity}
   :methodology/survey-id              {:db/unique :db.unique/identity}
   :risk-screen/survey-id              {:db/unique :db.unique/identity}
   :ledger/seq                        {:db/unique :db.unique/identity}
   :report/seq                        {:db/unique :db.unique/identity}
   :report-sequence/jurisdiction      {:db/unique :db.unique/identity}})

(defn- enc [v] (ls/enc v))
(defn- dec* [s] (ls/dec* s))

(defn- survey->tx [{:keys [id client-name actual-sample-size minimum-required-sample-size
                         unrepresentative-sample-risk-unresolved?
                         findings-report-published?
                         jurisdiction status report-number]}]
  (cond-> {:survey/id id}
    client-name                                     (assoc :survey/client-name client-name)
    actual-sample-size                              (assoc :survey/actual-sample-size actual-sample-size)
    minimum-required-sample-size                    (assoc :survey/minimum-required-sample-size minimum-required-sample-size)
    (some? unrepresentative-sample-risk-unresolved?) (assoc :survey/unrepresentative-sample-risk-unresolved? unrepresentative-sample-risk-unresolved?)
    (some? findings-report-published?)               (assoc :survey/findings-report-published? findings-report-published?)
    jurisdiction                                      (assoc :survey/jurisdiction jurisdiction)
    status                                            (assoc :survey/status status)
    report-number                                     (assoc :survey/report-number report-number)))

(def ^:private survey-pull
  [:survey/id :survey/client-name :survey/actual-sample-size :survey/minimum-required-sample-size
   :survey/unrepresentative-sample-risk-unresolved? :survey/findings-report-published?
   :survey/jurisdiction :survey/status :survey/report-number])

(defn- pull->survey [m]
  (when (:survey/id m)
    {:id (:survey/id m) :client-name (:survey/client-name m)
     :actual-sample-size (:survey/actual-sample-size m)
     :minimum-required-sample-size (:survey/minimum-required-sample-size m)
     :unrepresentative-sample-risk-unresolved? (boolean (:survey/unrepresentative-sample-risk-unresolved? m))
     :findings-report-published? (boolean (:survey/findings-report-published? m))
     :jurisdiction (:survey/jurisdiction m) :status (:survey/status m)
     :report-number (:survey/report-number m)}))

(defrecord DatomicStore [conn]
  Store
  (survey [_ id]
    (pull->survey (d/pull (d/db conn) survey-pull [:survey/id id])))
  (all-surveys [_]
    (->> (d/q '[:find [?id ...] :where [?e :survey/id ?id]] (d/db conn))
         (map #(pull->survey (d/pull (d/db conn) survey-pull [:survey/id %])))
         (sort-by :id)))
  (risk-screen-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?sid
                :where [?k :risk-screen/survey-id ?sid] [?k :risk-screen/payload ?p]]
              (d/db conn) id)))
  (methodology-of [_ survey-id]
    (dec* (d/q '[:find ?p . :in $ ?sid
                :where [?a :methodology/survey-id ?sid] [?a :methodology/payload ?p]]
              (d/db conn) survey-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (report-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :report/seq ?s] [?e :report/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-report-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :report-sequence/jurisdiction ?j] [?e :report-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (survey-already-published? [s survey-id]
    (boolean (:findings-report-published? (survey s survey-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :survey/upsert
      (d/transact! conn [(survey->tx value)])

      :methodology/set
      (d/transact! conn [{:methodology/survey-id (first path) :methodology/payload (enc payload)}])

      :risk-screen/set
      (d/transact! conn [{:risk-screen/survey-id (first path) :risk-screen/payload (enc payload)}])

      :survey/mark-published
      (let [survey-id (first path)
            {:keys [result survey-patch]} (publish-findings-report! s survey-id)
            jurisdiction (:jurisdiction (survey s survey-id))
            next-n (inc (next-report-sequence s jurisdiction))]
        (d/transact! conn
                     [(survey->tx (assoc survey-patch :id survey-id))
                      {:report-sequence/jurisdiction jurisdiction :report-sequence/next next-n}
                      {:report/seq (count (report-history s)) :report/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-surveys [s surveys]
    (when (seq surveys) (d/transact! conn (mapv survey->tx (vals surveys)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:surveys ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [surveys]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-surveys s surveys))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo survey set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
