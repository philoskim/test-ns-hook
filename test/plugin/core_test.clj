(ns plugin.core-test
  (:require [clojure.test :refer :all]))

(use 'debux.core)

(defn my-test-fixture [f]
  (print "start fixture")
  (f)
  (print "end fixture"))

(use-fixtures :once my-test-fixture)


(deftest test-a
  (testing "test-a"
    (is (= 1 1))
    (is (= (+ 2 2) 5)) ))

(deftest test-b
  (testing "test-b"
    (is (= 10 10))
    (is (= 20 20))))

(defn test-ns-hook []
  (test-a)
  (test-b))

