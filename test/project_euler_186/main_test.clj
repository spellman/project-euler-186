(ns project-euler-186.main-test
  (:require [clojure.edn :as edn]
            [clojure.test :as t :refer [deftest testing is are]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [project-euler-186.main :refer :all]
            [project-euler-186.algorithms :as alg]))

;; Here we test that command-line options are being handled correctly and that
;; the program starts and finishes correctly.

;; HELPERS
;; NOTE: that while we could factor out additional functionality from the
;; option-specific functions below, I'd rather not assume that the various
;; options will all continue to be so similar and change in lockstep.

(defn with-pm-arg
  ([x] (with-pm-arg x []))
  ([x args] (into args ["--prime-minister" x])))

(defn with-pf-arg
  ([x] (with-pf-arg x []))
  ([x args] (into args ["--percent-friends" x])))

(defn with-alg-arg
  ([x] (with-alg-arg x []))
  ([x args] (into args ["--algorithm" x])))

(defn try-read-string [s]
  (try
    (edn/read-string s)
    (catch Exception e s)))



;; COMMAND-LINE OPTION DEFAULTS

(deftest command-line-args-default-values
  (let [res (validate-args [])]
    (is (= (:prime-minister res) default-prime-minister))
    (is (= (:percent-friends res) default-percent-friends))
    (is (= (:algorithm res) default-algorithm))))



;; PRIME MINISTER COMMAND-LINE OPTION

(defn valid-pm-in-result [res pm]
  (is (->> res :prime-minister (= pm)))
  (is (-> res :exit-msg nil?)))

(defn valid-pm [pm-arg pm]
  (-> pm-arg with-pm-arg validate-args (valid-pm-in-result pm)))

(defn invalid-pm-in-result [res]
  (is (-> res :prime-minister nil?))
  (is (-> res :exit-msg string?))
  (is (not (:exit-ok? res))))

(defn invalid-pm [pm]
  (-> pm with-pm-arg validate-args invalid-pm-in-result))

(deftest prime-minister-must-be-a-user
  (testing "requires a value when the flag is specified"
    (invalid-pm-in-result (validate-args ["--prime-minister"])))

  (testing "must be an integer i st. 0 <= i <= 999999"
    (valid-pm "0" 0)
    (valid-pm "1" 1)
    (valid-pm "5" 5)
    (valid-pm "524287" 524287)
    (valid-pm "999999" 999999)

    (invalid-pm "-1")
    (invalid-pm "1000000")
    (invalid-pm "andy")
    (invalid-pm "")
    (invalid-pm "nil")))

(defspec prime-minister-must-be-a-user-spec
  (prop/for-all [pm-str (gen/fmap str
                                  (gen/one-of [gen/any gen/int gen/string-alphanumeric]))]
                (let [pm (try-read-string pm-str)]
                  (if (and (int? pm) (<= 0 pm 999999))
                    (valid-pm pm-str pm)
                    (invalid-pm pm-str)))))



;; PERCENT-FRIENDS COMMAND-LINE OPTION

(defn valid-pf-in-result [res pf]
  (is (->> res :percent-friends (= pf)))
  (is (-> res :exit-msg nil?)))

(defn valid-pf [pf-arg pf]
  (-> pf-arg with-pf-arg validate-args (valid-pf-in-result pf)))

(defn invalid-pf-in-result [res]
  (is (-> res :percent-friends nil?))
  (is (-> res :exit-msg string?))
  (is (not (:exit-ok? res))))

(defn invalid-pf [pf]
  (-> pf with-pf-arg validate-args invalid-pf-in-result))

(deftest percent-friends
  (testing "requires a value when the flag is specified"
    (invalid-pf-in-result (validate-args ["--percent-friends"])))

  (testing "must be an integer i st. 0 <= i <= 100"
    (valid-pf "0" 0)
    (valid-pf "1" 1)
    (valid-pf "5" 5)
    (valid-pf "100" 100)

    (invalid-pf "-1")
    (invalid-pf "101")
    (invalid-pf "quite a few")
    (invalid-pf "")
    (invalid-pf "nil")))

(defspec percent-friends-spec
  (prop/for-all [pf-str (gen/fmap str
                                  (gen/one-of [gen/any gen/int gen/string-alphanumeric]))]
                (let [pf (try-read-string pf-str)]
                  (if (and (int? pf) (<= 0 pf 999999))
                    (valid-pf pf-str pf)
                    (invalid-pf pf-str)))))



;; ALGORITHM

(defn valid-alg-in-result [res alg]
  (is (->> res :algorithm (= alg)))
  (is (-> res :exit-msg nil?)))

(defn valid-alg [alg]
  (-> alg with-alg-arg validate-args (valid-alg-in-result alg)))

(defn invalid-alg-in-result [res]
  (is (-> res :algorithm nil?))
  (is (-> res :exit-msg string?))
  (is (not (:exit-ok? res))))

(defn invalid-alg [alg]
  (-> alg with-alg-arg validate-args invalid-alg-in-result))

(deftest algorithm-command-line-arg
  (testing "command-line-option requires a value when the flag is specified"
    (invalid-alg-in-result (validate-args ["--algorithm"]))
    (invalid-alg-in-result (validate-args ["-a"])))

  (testing "command-line-option must be a supported algorithm, as explained in the command-line help"
    (valid-alg "graph-coloring")
    (valid-alg "disjoint-set")
    (valid-alg "disjoint-set+path-compress")
    (valid-alg "disjoint-set-atoms")
    (valid-alg "disjoint-set-atoms+path-compress")
    (valid-alg "disjoint-set-java+path-compress")

    (invalid-alg "")
    (invalid-alg "nil")))

(defspec algorithm-spec
  (prop/for-all [alg (gen/fmap str
                               (gen/one-of [gen/any gen/int gen/string-alphanumeric]))]
                (if (contains? algorithms alg)
                  (valid-alg alg)
                  (invalid-alg alg))))

(deftest get-algorithm-matches-names-to-functions
  (is (= #'alg/graph-coloring (get-algorithm "graph-coloring")))
  (is (= #'alg/disj-set (get-algorithm "disjoint-set")))
  (is (= #'alg/disj-set-p (get-algorithm "disjoint-set+path-compress")))
  (is (= #'alg/disj-set-a (get-algorithm "disjoint-set-atoms")))
  (is (= #'alg/disj-set-ap (get-algorithm "disjoint-set-atoms+path-compress")))
  (is (= #'alg/disj-set-jp (get-algorithm "disjoint-set-java+path-compress"))))

(defspec get-algorithm-defaults-to-graph-coloring-for-unknown-algorithm-name
  (prop/for-all [s (gen/such-that (complement (set algorithms))
                                  (gen/fmap str gen/any))]
                (= #'alg/graph-coloring (get-algorithm s))))



;; BENCHMARK

(deftest benchmark
  (testing "does not require a value and ignores any value given it"
    (is (= (validate-args ["--benchmark"]) (validate-args ["--benchmark" "value"])))
    (is (= (validate-args ["-b"]) (validate-args ["-b" "4"]))))

  (testing "signals that a benchmark should be run iff provided"
    (is (:benchmark (validate-args ["--benchmark"])))
    (is (:benchmark (validate-args ["-b"])))
    (is (not (:benchmark (validate-args []))))
    (is (not (:benchmark (validate-args ["-h"]))))
    (is (not (:benchmark (validate-args ["--prime-minister" "0"]))))
    (is (not (:benchmark (validate-args ["--percent-friends" "0"]))))
    (is (not (:benchmark (validate-args ["--algorithm" "0"]))))))



;; HELP

(deftest help
  (testing "does not require a value and ignores any value given it"
    (is (= (validate-args ["--help"]) (validate-args ["--help" "value"])))
    (is (= (validate-args ["-h"]) (validate-args ["-h" "4"]))))

  (testing "returns an explanation of the program and the options with which it may be run"
    (is (-> (validate-args ["--help"]) :exit-msg string?))
    (is (-> (validate-args ["-h"]) :exit-msg string?)))

  (testing "signals it is exiting without error"
    (is (:exit-ok? (validate-args ["--help"])))
    (is (:exit-ok? (validate-args ["-h"]))))

  (testing "overrides other args"
    (let [res (validate-args ["--help" "--unsupported-argument" "-a" "invalid-algorithm"])]
      (is (-> res :exit-msg string?))
      (is (:exit-ok? res)))))
