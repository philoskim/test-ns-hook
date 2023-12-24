(ns philoskim.kaocha.plugin.test-ns-hook
  (:require [kaocha.plugin :as plugin]
            [kaocha.report :as report]))

(use 'debux.core)

;;; plugin test-ns-hook
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
     :kaocha.testable/wrap [] }))

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


;;; outputs
(def orig-dots* (get-method report/dots* :error))
(defmethod report/dots* :error
  [{:keys [message file] :as m}]
  (when-not (and (= message "Uncaught exception, not in assertion.")
                 (= file "support.clj"))
    (orig-dots* m) ))

(def orig-fail-summary (get-method report/fail-summary :error))
(defmethod report/fail-summary :error
  [{:keys [message file testing-vars] :as m}]
  (when-not (and (= message "Uncaught exception, not in assertion.")
                 (= file "support.clj")
                 (re-find #"test-ns-hook$" (-> testing-vars last str)))
    (orig-fail-summary m) ))

(def orig-result (get-method report/result :summary))
(defmethod report/result :summary [m]
  (if (and (get-in m [:kaocha/test-plan :kaocha/fail-fast?])
           (pos-int? (:error m)))
    (orig-result (update m :error dec))
    (orig-result m) ))
