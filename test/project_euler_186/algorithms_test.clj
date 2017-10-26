(ns project-euler-186.algorithms-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [project-euler-186.algorithms :refer :all]))

;; From problem statement
(def prime-minister 524287)

(def first-calls
  [[200007 100053]
   [600183 500439]
   [600863 701497]])

(def first-fibs (reduce into [] first-calls))



(deftest first-generated-fibs-match-given
  (is (= (lagged-fib-generator 1) (first-fibs 0)))
  (is (= (lagged-fib-generator 2) (first-fibs 1)))
  (is (= (lagged-fib-generator 3) (first-fibs 2)))
  (is (= (lagged-fib-generator 4) (first-fibs 3)))
  (is (= (lagged-fib-generator 5) (first-fibs 4)))
  (is (= (lagged-fib-generator 6) (first-fibs 5))))

(deftest first-calls-match-given
  (is (= (first-calls 0) (make-call 1)))
  (is (= (first-calls 1) (make-call 2)))
  (is (= (first-calls 2) (make-call 3))))



;; As per Project Euler
(def answer 2325629)

(deftest graph-coloring-produces-the-correct-answer
  (is (= answer (graph-coloring 1000000 prime-minister 99)))
  (is (= 0 (graph-coloring 1000000 prime-minister 0))))

(deftest disj-set-produces-the-correct-answer
  (is (= answer (disj-set 1000000 prime-minister 99)))
  (is (= 0 (disj-set 1000000 prime-minister 0))))

(deftest disj-set-p-produces-the-correct-answer
  (is (= answer (disj-set-p 1000000 prime-minister 99)))
  (is (= 0 (disj-set-p 1000000 prime-minister 0))))

(deftest disj-set-a-produces-the-correct-answer
  (is (= answer (disj-set-a 1000000 prime-minister 99)))
  (is (= 0 (disj-set-a 1000000 prime-minister 0))))

(deftest disj-set-ap-produces-the-correct-answer
  (is (= answer (disj-set-ap 1000000 prime-minister 99)))
  (is (= 0 (disj-set-ap 1000000 prime-minister 0))))

(deftest disj-set-jp-produces-the-correct-answer
  (is (= answer (disj-set-jp 1000000 prime-minister 99)))
  (is (= 0 (disj-set-jp 1000000 prime-minister 0))))



(deftest graph-coloring-num-calls-grows-with-percent-of-users
  (is (<= (graph-coloring 1000000 prime-minister 5)
          (graph-coloring 1000000 prime-minister 50)
          (graph-coloring 1000000 prime-minister 90))))

(deftest disj-set-num-calls-grows-with-percent-of-users
  (is (<= (disj-set 1000000 prime-minister 5)
          (disj-set 1000000 prime-minister 50)
          (disj-set 1000000 prime-minister 90))))

(deftest disj-set-p-num-calls-grows-with-percent-of-users
  (is (<= (disj-set-p 1000000 prime-minister 5)
          (disj-set-p 1000000 prime-minister 50)
          (disj-set-p 1000000 prime-minister 90))))

(deftest disj-set-a-num-calls-grows-with-percent-of-users
  (is (<= (disj-set-a 1000000 prime-minister 5)
          (disj-set-a 1000000 prime-minister 50)
          (disj-set-a 1000000 prime-minister 90))))

(deftest disj-set-ap-num-calls-grows-with-percent-of-users
  (is (<= (disj-set-ap 1000000 prime-minister 5)
          (disj-set-ap 1000000 prime-minister 50)
          (disj-set-ap 1000000 prime-minister 90))))

(deftest disj-set-jp-num-calls-grows-with-percent-of-users
  (is (<= (disj-set-jp 1000000 prime-minister 5)
          (disj-set-jp 1000000 prime-minister 50)
          (disj-set-jp 1000000 prime-minister 90))))
