(ns polling.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 for this repo: it previously had NO
  demo page and no generator at all. This namespace drives the REAL
  actor stack (`polling.operation` -> `polling.governor` ->
  `polling.store`) through a scenario taken directly from this repo's
  own `polling.sim` demo driver (`clojure -M:dev:run`), which was run
  and its printed audit ledger inspected before this file was written
  and confirmed to use ids that DO match `polling.store/demo-data`
  (survey-1..survey-4) and to actually produce every disposition it
  claims -- unlike `cloud-itonami-isic-851`'s `schoolops.sim`, this
  repo's own sim driver is clean, so it was safe to reuse rather than
  author a scenario from scratch. Rendered deterministically -- no
  invented numbers, no timestamps in the page content, byte-identical
  across reruns against the same seed (verified by diffing two
  consecutive runs).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [polling.store :as store]
            [polling.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator
  {:actor-id "op-1" :actor-role :research-operator :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing every
  disposition this actor can reach: survey-1 clears intake
  (auto-commit -- the only auto-eligible op at phase 3, no capital
  risk), then a methodology/verify (ALWAYS escalates -- approved), a
  risk/screen finding no unresolved risk (ALWAYS escalates --
  approved), and a findings-report publication (ALWAYS escalates --
  approved, actually mints a report record via
  `polling.registry/register-findings-report`); survey-2 HARD-holds a
  methodology/verify with no jurisdiction spec-basis; survey-3 clears
  its own methodology/verify (approved) but then HARD-holds a
  findings-report publication because its own actual sample size
  (600) falls short of its own recorded minimum-required sample size
  (1000); survey-4 HARD-holds a risk/screen because the survey's own
  seeded record already carries an unresolved unrepresentative-sample
  risk; finally survey-1 HARD-holds a second findings-report
  publication attempt (double-publication guard). Every HARD hold
  never reaches a human. Returns the resulting store -- every field
  read by `render` below is real governor/store output, not a
  hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "s1-intake" {:op :survey/intake :subject "survey-1"
                               :patch {:id "survey-1" :client-name "Sato Retail Group"}})

    (exec! actor "s1-methodology" {:op :methodology/verify :subject "survey-1"})
    (approve! actor "s1-methodology")

    (exec! actor "s1-risk" {:op :risk/screen :subject "survey-1"})
    (approve! actor "s1-risk")

    (exec! actor "s1-publish" {:op :actuation/publish-findings-report :subject "survey-1"})
    (approve! actor "s1-publish")

    (exec! actor "s2-methodology" {:op :methodology/verify :subject "survey-2" :no-spec? true})

    (exec! actor "s3-methodology" {:op :methodology/verify :subject "survey-3"})
    (approve! actor "s3-methodology")

    (exec! actor "s3-publish" {:op :actuation/publish-findings-report :subject "survey-3"})

    (exec! actor "s4-risk" {:op :risk/screen :subject "survey-4"})

    (exec! actor "s1-publish-again" {:op :actuation/publish-findings-report :subject "survey-1"})
    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- last-fact-for [ledger survey-id]
  (last (filter #(= (:subject %) survey-id) ledger)))

(defn- status-cell [ledger survey-id]
  (let [f (last-fact-for ledger survey-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (-> f :violations first :rule)]
        (str "<span class=\"critical\">HARD hold &middot; " (esc (name (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- survey-row [ledger {:keys [id client-name jurisdiction actual-sample-size
                                  minimum-required-sample-size status]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id) (esc client-name) (esc jurisdiction)
          (esc (str actual-sample-size "/" minimum-required-sample-size))
          (esc (name (or status :n-a)))
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis reason by]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc subject)
          (esc (or (some->> basis seq (map name) (str/join ", "))
                    (some-> disposition name)
                    (some-> reason name)
                    (some->> by (str "approved-by: "))
                    ""))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own op contract (README `Run`
  ;; table, `polling.governor`/`polling.phase`) -- documentation of
  ;; fixed behavior, not runtime telemetry, so it is legitimately
  ;; hand-described rather than derived from a live run.
  ["        <tr><td><code>:survey/intake</code></td><td><span class=\"ok\">auto-commit when clean, phase 3, no capital risk</span></td></tr>"
   "        <tr><td><code>:methodology/verify</code></td><td><span class=\"warn\">ALWAYS human approval &middot; HARD-holds on a fabricated/missing jurisdiction spec-basis</span></td></tr>"
   "        <tr><td><code>:risk/screen</code></td><td><span class=\"warn\">ALWAYS human approval &middot; can HARD-hold on its own unresolved-risk finding</span></td></tr>"
   "        <tr><td><code>:actuation/publish-findings-report</code></td><td><span class=\"warn\">ALWAYS human approval &middot; independent sample-size floor, evidence-completeness and double-publication checks</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        surveys (->> (store/all-surveys db)
                     (filter #(#{"survey-1" "survey-2" "survey-3" "survey-4"} (:id %)))
                     (sort-by :id))
        survey-rows (str/join "\n" (map (partial survey-row ledger) surveys))
        ledger-rows (str/join "\n" (map ledger-row ledger))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-7320 &middot; market-research &amp; public-opinion polling</title><style>\n"
     "table { width: 100%; border-collapse: collapse; font-size: 14px; }\n"
     ".ok { color: #137a3f; }\n"
     "body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }\n"
     "header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }\n"
     "th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }\n"
     "h2 { margin-top: 0; font-size: 15px; }\n"
     ".warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }\n"
     "main { max-width: 980px; margin: 24px auto; padding: 0 20px; }\n"
     "header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }\n"
     ".muted { color: #888; font-size: 13px; }\n"
     ".critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }\n"
     ".card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }\n"
     ".err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }\n"
     "th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }\n"
     "header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }\n"
     "code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }\n"
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Market-research &amp; public-opinion polling (ISIC 7320) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · findings-report publication always human-approved</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Surveys</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>polling.store</code> via <code>polling.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Survey</th><th>Client</th><th>Jurisdiction</th><th>Sample (actual/min)</th><th>Status</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     survey-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Survey Integrity Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden. Sample-size sufficiency is independently recomputed, never trusted from the proposal; a findings report can never be published twice for the same survey.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Subject</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/report-history db)) "findings-report records )")))
