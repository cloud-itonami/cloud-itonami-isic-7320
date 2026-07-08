(ns polling.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean survey through
  intake -> methodology verification -> unrepresentative-sample-risk
  screening -> findings-report-publication proposal (always
  escalates) -> human approval -> commit, then shows four HARD holds
  (a jurisdiction with no spec-basis, an undersized sample below its
  own recorded minimum-required sample size, an unresolved
  unrepresentative-sample risk screened directly via `:risk/screen`
  [never via an actuation op against an unscreened survey -- see this
  actor's own governor ns docstring / the lesson `parksafety`'s
  ADR-2607071922 Decision 5, `eldercare`'s, `museum`'s,
  `conservation`'s, `salon`'s, `entertainment`'s, `casework`'s,
  `hospital`'s, `facility`'s, `school`'s, `association`'s, `leasing`'s,
  `behavioral`'s, `secondary`'s, `card`'s, `water`'s, `telecom`'s,
  `aerospace`'s, `recovery`'s, `consulting`'s, `union`'s,
  `congregation`'s, `fab`'s, `energy`'s, `care`'s, `navigator`'s,
  `learning`'s, `banking`'s and `advertising`'s ADR-0001s already
  recorded], and a double publication of an already-processed survey)
  that never reach a human at all, and prints the audit ledger + the
  draft findings-report records."
  (:require [langgraph.graph :as g]
            [polling.store :as store]
            [polling.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :research-operator :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== survey/intake survey-1 (JPN, clean; sample 1200 >= minimum 1000, no unrepresentative-sample risk) ==")
    (println (exec! actor "t1" {:op :survey/intake :subject "survey-1"
                                :patch {:id "survey-1" :client-name "Sato Retail Group"}} operator))

    (println "== methodology/verify survey-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :methodology/verify :subject "survey-1"} operator))
    (println (approve! actor "t2"))

    (println "== risk/screen survey-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :risk/screen :subject "survey-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/publish-findings-report survey-1 (always escalates -- actuation/publish-findings-report) ==")
    (let [r (exec! actor "t4" {:op :actuation/publish-findings-report :subject "survey-1"} operator)]
      (println r)
      (println "-- human research-operator approves --")
      (println (approve! actor "t4")))

    (println "== methodology/verify survey-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t5" {:op :methodology/verify :subject "survey-2" :no-spec? true} operator))

    (println "== methodology/verify survey-3 (escalates -- human approves; sets up the sample-size-insufficient test) ==")
    (println (exec! actor "t6" {:op :methodology/verify :subject "survey-3"} operator))
    (println (approve! actor "t6"))

    (println "== actuation/publish-findings-report survey-3 (sample 600 < minimum 1000 -> HARD hold) ==")
    (println (exec! actor "t7" {:op :actuation/publish-findings-report :subject "survey-3"} operator))

    (println "== risk/screen survey-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :risk/screen :subject "survey-4"} operator))

    (println "== actuation/publish-findings-report survey-1 AGAIN (double-publication -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/publish-findings-report :subject "survey-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft findings-report records ==")
    (doseq [r (store/report-history db)] (println r))))
