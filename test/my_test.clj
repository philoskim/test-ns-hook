(ns my-test
  (:require [clojure.test :refer :all]))

(defn my-test-fixture [f]
  (print "start fixture")
  (f)
  (print "end fixture"))

(use-fixtures :once my-test-fixture)


(deftest test-a
  (testing "test-a"
    ;(is (= (/ 10 0) 0))
    (is (= 1 1))
    (is (= (+ 2 3) 4)) ))

(deftest test-b
  (testing "test-b"
    (is (= 10 10))
    (is (= 20 20))))

(defn test-ns-hook []
  (test-a)
  (test-b))

