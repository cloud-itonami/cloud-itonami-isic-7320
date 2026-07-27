(ns polling.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [polling.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:provenance (facts/spec-basis "JPN")))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "JPN"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "JPN")]
    (is (facts/required-evidence-satisfied? "JPN" all))
    (is (not (facts/required-evidence-satisfied? "JPN" (rest all))))
    (is (not (facts/required-evidence-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))

(deftest chn-has-a-cited-spec-basis
  (let [sb (facts/spec-basis "CHN")]
    (is (some? sb))
    (is (re-find #"^http://www\.camir\.org/" (:provenance sb))
        "cites the CAMIR page that was actually fetched")
    (is (re-find #"^https://www\.cac\.gov\.cn/" (:pipl-provenance sb))
        "and the official PIPL text")
    (is (= "2026-07-27" (:retrieved-at sb))
        "records when the citations were read, so staleness is visible")
    (is (= 4 (count (facts/evidence-checklist "CHN"))))
    (is (facts/required-evidence-satisfied? "CHN" (facts/evidence-checklist "CHN")))))

(deftest chn-row-names-what-this-actor-does-not-screen
  (testing "the 涉外调查 permit/approval regime is a HARD gate this actor does not model"
    (let [sb (facts/spec-basis "CHN")]
      (is (= [:foreign-related-survey-permit
              :foreign-related-social-survey-approval]
             (:out-of-scope-here sb))
          "the boundary is declared in data, not only in a comment")
      (is (re-find #"cloud-itonami-iso3166-chn-market-research" (:out-of-scope-note sb))
          "and points at the actor that does implement it"))))

(deftest every-catalog-row-carries-a-provenance-url
  (doseq [[iso3 sb] facts/catalog]
    (is (string? (:provenance sb)) (str iso3 " has a provenance string"))
    (is (re-find #"^https?://" (:provenance sb)) (str iso3 " provenance is a URL"))
    (is (= 4 (count (:required-evidence sb))) (str iso3 " has the 4-item evidence set"))))
