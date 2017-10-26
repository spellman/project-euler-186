(ns project-euler-186.main
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [project-euler-186.algorithms :as alg]
            [criterium.core :as c])
  (:gen-class))

(defn make-conformer [spec xf]
  (fn [val]
    (let [conformed (s/conform spec (xf val))]
      (if (= conformed ::s/invalid)
        (throw
         (ex-info (s/explain spec conformed) (s/explain-data spec conformed)))
        conformed))))

(def default-prime-minister 524287)
(def default-percent-friends 99)
(def algorithms ["graph-coloring"
                 "disjoint-set"
                 "disjoint-set+path-compress"
                 "disjoint-set-atoms"
                 "disjoint-set-atoms+path-compress"
                 "disjoint-set-java+path-compress"])
(def default-algorithm "graph-coloring")

(defn indent-each-on-new-line [strs indent]
  (->> strs
       (interleave (repeat indent))
       (partition 2)
       (map (partial string/join ""))
       (string/join "\n")))

;; (*) 2017-10-25 Cort Spellman
;; There is a bug in s/int-in-range?: https://dev.clojure.org/jira/browse/CLJ-2167
;; The function int? is being checked for truthiness; not the result of applying
;; int? to the arg:
;; https://github.com/clojure/clojure/blob/d920ada9fab7e9b8342d28d8295a600a814c1d8a/src/clj/clojure/spec.clj#L1618
;; TODO: Remove the extra int? check once int-in-range? is fixed.

(def cli-options
  (let [description-alignment-indent (string/join "" (repeat 61 " "))]
    [[nil "--prime-minister PRIME-MINISTER"
      "The number of the prime minister. int <- [0, 999,999]"
      :default 524287
      :parse-fn edn/read-string
      :validate-fn #(and (int? %) (s/int-in-range? 0 1000000 %)) ; See (*) above.
      :validate-msg "Must be an integer, i, s.t. 0 <= i <= 999,999."]
     [nil "--percent-friends PERCENT-FRIENDS"
      "Threshold percentage of friends at which to stop and return the result. num <- [0, 100]"
      :default 99
      :parse-fn edn/read-string
      :validate-fn #(<= 0 % 100)
      :validate-msg "Must be a number, i, s.t. 0 <= i <= 100."]
     ["-a" "--algorithm ALGORITHM"
      (str "The friend-counting algorithm to use. The following are supported:\n"
           (indent-each-on-new-line algorithms description-alignment-indent))
      :default "graph-coloring"
      :validate-fn #(contains? (set algorithms) %)
      :validate-msg (str "Must be one of\n" (string/join "\n" algorithms))]
     ["-b" "--benchmark"
      "Runs a benchmark (with Criterium quick-bench)."]
     ["-h" "--help"
      "Prints a description of the program and its usage."]]))

(defn usage [options-summary]
  (str
   "
This program solves Project Euler problem 186: https://projecteuler.net/problem=186:


PROBLEM SETUP
=============
Here are the records from a busy telephone system with one million users:

RecNr  Caller  Called
    1  200007  100053
    2  600183  500439
    3  600863  701497
  ...     ...     ...

The telephone number of the caller and the called number in record n are Caller(n) = S2n-1 and Called(n) = S2n where S1,2,3,... come from the \"Lagged Fibonacci Generator\":

For 1 ≤ k ≤ 55, Sk = [100003 - 200003k + 300007k3] (modulo 1000000)
For 56 ≤ k, Sk = [Sk-24 + Sk-55] (modulo 1000000)

If Caller(n) = Called(n) then the user is assumed to have misdialled and the call fails; otherwise the call is successful.

From the start of the records, we say that any pair of users X and Y are friends if X calls Y or vice-versa. Similarly, X is a friend of a friend of Z if X is a friend of Y and Y is a friend of Z; and so on for longer chains.

The Prime Minister's phone number is 524287.


PROBLEM STATEMENT
=================
After how many successful calls, not counting misdials, will 99% of the users (including the PM) be a friend, or a friend of a friend etc., of the Prime Minister?


PROGRAM USAGE
=============
Run the program with no arguments to use the values given in the problem for the Prime Minister and threshold percentage of users or specify your own values with flags:\n\n"
   options-summary))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join "\n" errors)))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) {:exit-msg (usage summary) :exit-ok? true}
      errors {:exit-msg (error-msg errors)}
      :else (select-keys options [:prime-minister :percent-friends :algorithm :benchmark]))))

(defn get-algorithm [s]
  (case s
    "disjoint-set" #'alg/disj-set
    "disjoint-set+path-compress" #'alg/disj-set-p
    "disjoint-set-atoms" #'alg/disj-set-a
    "disjoint-set-atoms+path-compress" #'alg/disj-set-ap
    "disjoint-set-java+path-compress" #'alg/disj-set-jp
    #'alg/graph-coloring))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  [& args]
  (let [num-users 1000000
        {:keys [exit-msg exit-ok? prime-minister percent-friends algorithm benchmark]}
        (validate-args args)]
    (if exit-msg
      (exit (if exit-ok? 0 1) exit-msg)
      (let [algo (get-algorithm algorithm)]
        (if benchmark
          (do
            (println "Benchmarking. This will take a few minutes...")
            (println (c/quick-bench (algo num-users prime-minister percent-friends))))
          (do
            (println "Running. This will take a short time...")
            (println (algo num-users prime-minister percent-friends))))))))
