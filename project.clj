(defproject project-euler-186 "0.1.0"
  :description "Solution to Project Euler problem 186: https://projecteuler.net/problem=186"
  :url "https://github.com/spellman/project-euler-186"
  :license {:name "public domain"
            :url "https://unlicense.org/"}
  :dependencies [[org.clojure/clojure "1.9.0-beta2"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/algo.generic "0.1.2"]
                 [criterium "0.4.4"]]
  :main ^:skip-aot project-euler-186.main
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
