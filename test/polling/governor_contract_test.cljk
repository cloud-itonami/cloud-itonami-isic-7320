(ns polling.governor-contract-test
  "The governor contract as executable tests -- the polling analog of
  `cloud-itonami-isic-6512`'s `casualty.governor-contract-test`. The
  single invariant under test:

    PollOps-LLM never publishes a findings report the Survey
    Integrity Governor would reject, `:actuation/publish-findings-
    report` NEVER auto-commits at any phase, `:survey/intake` (no
    direct capital risk) MAY auto-commit when clean, and every
    decision (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [polling.store :as store]
            [polling.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :research-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a methodology
  assessment on file. Uses distinct thread-ids per call site by
  suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :methodology/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :survey/intake :subject "survey-1"
                   :patch {:id "survey-1" :client-name "Sato Retail Group"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sato Retail Group" (:client-name (store/survey db "survey-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest methodology-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :methodology/verify :subject "survey-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/methodology-of db "survey-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a methodology/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :methodology/verify :subject "survey-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/methodology-of db "survey-1")) "no methodology assessment written"))))

(deftest publish-findings-report-without-methodology-is-held
  (testing "actuation/publish-findings-report before any methodology verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/publish-findings-report :subject "survey-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest sample-size-insufficient-is-held
  (testing "a survey whose own actual sample size falls short of its own minimum-required sample size -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "survey-3")
          res (exec-op actor "t5" {:op :actuation/publish-findings-report :subject "survey-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:sample-size-insufficient} (-> (store/ledger db) last :basis)))
      (is (empty? (store/report-history db))))))

(deftest unrepresentative-sample-risk-is-held-and-unoverridable
  (testing "an unresolved unrepresentative-sample risk on a survey -> HOLD, and never reaches request-approval -- exercised via :risk/screen DIRECTLY, not via the actuation op against an unscreened survey (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's, recovery's, consulting's, union's, congregation's, fab's, energy's, care's, navigator's, learning's, banking's and advertising's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :risk/screen :subject "survey-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:unrepresentative-sample-risk-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/risk-screen-of db "survey-4")) "no clearance written"))))

(deftest publish-findings-report-always-escalates-then-human-decides
  (testing "a clean, fully-assessed survey still ALWAYS interrupts for human approval -- actuation/publish-findings-report is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "survey-1")
          r1 (exec-op actor "t7" {:op :actuation/publish-findings-report :subject "survey-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, report record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:findings-report-published? (store/survey db "survey-1"))))
          (is (= 1 (count (store/report-history db))) "one draft report record"))))))

(deftest publish-findings-report-double-publication-is-held
  (testing "publishing the same survey's findings report twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "survey-1")
          _ (exec-op actor "t8a" {:op :actuation/publish-findings-report :subject "survey-1"} operator)
          _ (approve! actor "t8a")
          res (exec-op actor "t8" {:op :actuation/publish-findings-report :subject "survey-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-published} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/report-history db))) "still only the one earlier publication"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :survey/intake :subject "survey-1"
                          :patch {:id "survey-1" :client-name "Sato Retail Group"}} operator)
      (exec-op actor "b" {:op :methodology/verify :subject "survey-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
