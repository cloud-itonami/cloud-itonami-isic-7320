(ns polling.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:actuation/publish-findings-report` must NEVER be a
  member of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [polling.phase :as phase]))

(deftest publish-findings-report-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real findings-report publication"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/publish-findings-report))
          (str "phase " n " must not auto-commit :actuation/publish-findings-report")))))

(deftest risk-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :risk/screen))
          (str "phase " n " must not auto-commit :risk/screen")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":survey/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:survey/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :survey/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/publish-findings-report} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :survey/intake} :commit)))))
