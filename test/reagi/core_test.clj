(ns reagi.core-test
  (:use clojure.test)
  (:require [reagi.core :as r]))

(deftest test-behavior
  (let [a (atom 1)
        b (r/behavior (+ 1 @a))]
    (is (= @b 2))
    (swap! a inc)
    (is (= @b 3))))

(deftest test-event-stream
  (testing "Initial value"
    (is (nil? @(r/event-stream)))
    (is (= 1 @(r/event-stream 1))))
  (testing "Push"
    (let [e (r/event-stream)]
      (e 1)
      (is (= 1 @e))
      (e 2)
      (is (= 2 @e)))))

(deftest test-push!
  (let [e (r/event-stream)]
    (r/push! e 1)
    (is (= 1 @e))
    (r/push! e 2 3 4)
    (is (= 4 @e))))

(deftest test-freeze
  (let [f (r/freeze (r/event-stream))]
    (is (r/frozen? f))
    (is (thrown? ClassCastException (r/push! f 1)))))

(deftest test-mapcat
  (let [s (r/event-stream)
        e (r/mapcat (fn [x] [(inc x)]) s)]
    (r/push! s 1)
    (is (= 2 @e))))

(deftest test-map
  (let [s (r/event-stream)
        e (r/map inc 1 s)]
    (is (= 1 @e))
    (r/push! s 1)
    (is (= 2 @e))))

(deftest test-filter-by
  (let [s (r/event-stream)
        e (r/filter-by {:type :key-pressed} s)]
    (r/push! s {:type :key-pressed :key :a})
    (r/push! s {:type :key-released :key :a})
    (is (= @e {:type :key-pressed :key :a}))))

(deftest test-uniq
  (let [s (r/event-stream)
        e (r/reduce + 0 (r/uniq s))]
    (r/push! s 1 1)
    (is (= 1 @e))
    (r/push! s 1 2)
    (is (= 3 @e))))

(deftest test-count
  (let [e (r/event-stream)
        c (r/count e)]
    (is (= @c 0))
    (r/push! e 1)
    (is (= @c 1))
    (r/push! e 2 3)
    (is (= @c 3))))

(deftest test-cycle
  (let [s (r/event-stream)
        e (r/cycle [:on :off] s)]
    (is (= :on @e))
    (r/push! s 1)
    (is (= :off @e))
    (r/push! s 1)
    (is (= :on @e))))

(deftest test-gc
  (testing "Derived maps"
    (let [s (r/event-stream)
          e (r/map inc (r/map inc s))]
      (System/gc)
      (r/push! s 1)
      (is (= @e 3))))
  (testing "Merge"
    (let [s (r/event-stream)
          e (r/merge (r/map inc s))]
      (System/gc)
      (r/push! s 1)
      (is (= @e 2))))
  (testing "GC unreferenced streams"
    (let [a (atom nil)
          s (r/event-stream)]
      (r/map #(reset! a %) s)
      (System/gc)
      (r/push! s 1)
      (is (nil? @a)))))