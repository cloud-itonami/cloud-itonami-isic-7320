(ns polling.registry-test
  (:require [clojure.test :refer [deftest is]]
            [polling.registry :as r]))

;; ----------------------------- sample-size-insufficient? -----------------------------

(deftest not-insufficient-when-meets-minimum
  (is (not (r/sample-size-insufficient? {:actual-sample-size 1200 :minimum-required-sample-size 1000})))
  (is (not (r/sample-size-insufficient? {:actual-sample-size 1000 :minimum-required-sample-size 1000}))))

(deftest insufficient-when-below-minimum
  (is (r/sample-size-insufficient? {:actual-sample-size 600 :minimum-required-sample-size 1000}))
  (is (r/sample-size-insufficient? {:actual-sample-size 999 :minimum-required-sample-size 1000})))

(deftest insufficient-is-false-on-missing-fields
  (is (not (r/sample-size-insufficient? {})))
  (is (not (r/sample-size-insufficient? {:actual-sample-size 600}))))

;; ----------------------------- register-findings-report -----------------------------

(deftest report-is-a-draft-not-a-real-publication
  (let [result (r/register-findings-report "survey-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest report-assigns-report-number
  (let [result (r/register-findings-report "survey-1" "JPN" 7)]
    (is (= (get result "report_number") "JPN-RPT-000007"))
    (is (= (get-in result ["record" "survey_id"]) "survey-1"))
    (is (= (get-in result ["record" "kind"]) "findings-report-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest report-validation-rules
  (is (thrown? Exception (r/register-findings-report "" "JPN" 0)))
  (is (thrown? Exception (r/register-findings-report "survey-1" "" 0)))
  (is (thrown? Exception (r/register-findings-report "survey-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-findings-report "survey-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-findings-report "survey-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-RPT-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-RPT-000001" (get-in hist2 [1 "record_id"])))))
