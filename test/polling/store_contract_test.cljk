(ns polling.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [polling.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Retail Group" (:client-name (store/survey s "survey-1"))))
      (is (= "JPN" (:jurisdiction (store/survey s "survey-1"))))
      (is (= 1200 (:actual-sample-size (store/survey s "survey-1"))))
      (is (= 1000 (:minimum-required-sample-size (store/survey s "survey-1"))))
      (is (false? (:unrepresentative-sample-risk-unresolved? (store/survey s "survey-1"))))
      (is (= 600 (:actual-sample-size (store/survey s "survey-3"))))
      (is (true? (:unrepresentative-sample-risk-unresolved? (store/survey s "survey-4"))))
      (is (false? (:findings-report-published? (store/survey s "survey-1"))))
      (is (= ["survey-1" "survey-2" "survey-3" "survey-4"]
             (mapv :id (store/all-surveys s))))
      (is (nil? (store/risk-screen-of s "survey-1")))
      (is (nil? (store/methodology-of s "survey-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/report-history s)))
      (is (zero? (store/next-report-sequence s "JPN")))
      (is (false? (store/survey-already-published? s "survey-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :survey/upsert
                                 :value {:id "survey-1" :client-name "Sato Retail Group"}})
        (is (= "Sato Retail Group" (:client-name (store/survey s "survey-1"))))
        (is (= 1000 (:minimum-required-sample-size (store/survey s "survey-1"))) "unrelated field preserved"))
      (testing "methodology / risk-screen payloads commit and read back"
        (store/commit-record! s {:effect :methodology/set :path ["survey-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/methodology-of s "survey-1")))
        (store/commit-record! s {:effect :risk-screen/set :path ["survey-1"]
                                 :payload {:survey-id "survey-1" :verdict :resolved}})
        (is (= {:survey-id "survey-1" :verdict :resolved} (store/risk-screen-of s "survey-1"))))
      (testing "findings report drafts a record and advances the sequence"
        (store/commit-record! s {:effect :survey/mark-published :path ["survey-1"]})
        (is (= "JPN-RPT-000000" (get (first (store/report-history s)) "record_id")))
        (is (= "findings-report-draft" (get (first (store/report-history s)) "kind")))
        (is (true? (:findings-report-published? (store/survey s "survey-1"))))
        (is (= 1 (count (store/report-history s))))
        (is (= 1 (store/next-report-sequence s "JPN")))
        (is (true? (store/survey-already-published? s "survey-1")))
        (is (false? (store/survey-already-published? s "survey-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/survey s "nope")))
    (is (= [] (store/all-surveys s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/report-history s)))
    (is (zero? (store/next-report-sequence s "JPN")))
    (store/with-surveys s {"x" {:id "x" :client-name "n"
                               :actual-sample-size 1200 :minimum-required-sample-size 1000
                               :unrepresentative-sample-risk-unresolved? false
                               :findings-report-published? false
                               :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:client-name (store/survey s "x"))))))
