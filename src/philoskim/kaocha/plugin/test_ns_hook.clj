(ns philoskim.kaocha.plugin.test-ns-hook
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [kaocha.plugin :as plugin]
            [kaocha.report :as report]
            [kaocha.core-ext :as kce]
            [kaocha.output :as output]
            [kaocha.stacktrace :as stacktrace]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.history :as history]))

(use 'debux.core)

(defn- has-test-ns-hook? [ns]
  (some (fn [[symbol var]]
          (= symbol 'test-ns-hook))
        (ns-publics ns)))

(defn- remove-fixtures [ns-meta]
  (dissoc ns-meta
          :clojure.test/once-fixtures
          :clojure.test/each-fixtures))

(defn- create-test-case [ns]
  (let [ns-str (-> ns ns-name name)
        hook-var (ns-resolve ns 'test-ns-hook)
        hook-meta (-> hook-var
                      meta
                      (dissoc :arglists)
                      (assoc :test @hook-var))
        hook-str "test-ns-hook"]
    {:kaocha.testable/type :kaocha.type/var,
     :kaocha.testable/id (keyword ns-str hook-str)
     :kaocha.testable/meta hook-meta
     :kaocha.testable/desc hook-str
     :kaocha.var/name (symbol ns-str hook-str)
     :kaocha.var/var hook-var
     :kaocha.var/test (:test hook-meta)
     :kaocha.testable/wrap []}))

(defn- update-test-suite
  [{ns :kaocha.ns/ns :as test-suite}]
  (if (has-test-ns-hook? ns)
    (-> test-suite
        (update :kaocha.testable/meta remove-fixtures)
        (assoc :kaocha.test-plan/tests (list (create-test-case ns))))
    test-suite))

(defn- update-test-id [test-id]
  (update test-id :kaocha.test-plan/tests
          (fn [test-suites]
            (map update-test-suite test-suites) )))


(plugin/defplugin :philoskim.kaocha.plugin/test-ns-hook
  (pre-run [config]
           (update config :kaocha.test-plan/tests
                   (fn [test-ids]
                     (map update-test-id test-ids) ))))

(defmethod report/fail-summary :error-default
  [{:keys [testing-contexts] :as m}]
  (println (str "\n" (output/colored :red "ERROR") " in") (report/testing-vars-str m))
  (when (seq testing-contexts)
    (println (str/join " " (reverse testing-contexts))))
  (when-let [message (:message m)]
    (println message))
  (if-let [expr (::printed-expression m)]
    (print expr)
    (when-let [actual (:actual m)]
      (print "Exception: ")
      (if (kce/throwable? actual)
        (stacktrace/print-cause-trace actual t/*stack-trace-depth*)
        (prn actual))))
  (report/print-output m))

(defmethod report/fail-summary :error
  [{:keys [message file testing-vars] :as m}]
  (when-not (and (= message "Uncaught exception, not in assertion.")
                 (= file "support.clj")
                 (re-find #"test-ns-hook$" (-> testing-vars last str)))
    (report/fail-summary (assoc m :type :error-default)) ))


(defmethod report/dots* :error-default [_]
  (t/with-test-out
    (print (output/colored :red "E"))
    (flush)))

(defmethod report/dots* :error
  [{:keys [message file] :as m}]
  (when-not (and (= message "Uncaught exception, not in assertion.")
                 (= file "support.clj"))
    (report/dots* (assoc m :type :error-default)) ))

(defmethod report/result :summary-default [m]
  (let [history @history/*history*]
    (t/with-test-out
      (let [failures (filter hierarchy/fail-type? history)]
        (doseq [{:keys [testing-contexts testing-vars] :as m} failures]
          (binding [t/*testing-contexts* testing-contexts
                    t/*testing-vars* testing-vars]
            (report/fail-summary m))))

      (doseq [deferred (filter hierarchy/deferred? history)]
        (report/clojure-test-report deferred))

      (let [{:keys [test pass fail error pending] :or {pass 0 fail 0 error 0 pending 0}} m
            failed? (pos-int? (+ fail error))
            pending? (pos-int? pending)]
        (println (output/colored (if failed? :red (if pending? :yellow :green))
                                 (str test " tests, "
                                      (+ pass fail error) " assertions, "
                                      (when (pos-int? error)
                                        (str error " errors, "))
                                      (when pending?
                                        (str pending " pending, "))
                                      fail " failures."))))

      (when-let [pending (seq (filter hierarchy/pending? history))]
        (println)
        (doseq [m pending]
          (println (output/colored :yellow
                                   (str "PENDING " (report/testing-vars-str m)))))))))

(defmethod report/result :summary [m]
  (report/result (-> m
                     (update :error dec)
                     (assoc :type :summary-default))))

