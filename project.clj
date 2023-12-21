(defproject philoskim/test-ns-hook "0.1.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [lambdaisland/kaocha "1.87.1366"]
                 [philoskim/debux "0.9.1"]]
  :aliases {"unit-test" ["run" "-m" "kaocha.runner" ":unit"]})
