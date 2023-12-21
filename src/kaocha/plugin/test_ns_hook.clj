(ns kaocha.plugin.test-ns-hook
  (:require [kaocha.plugin :as plugin]))

(use 'debux.core)

(defmethod plugin/-register :kaocha.plugin/test-ns-hook
  [_name plugins]
  (conj plugins
        {:kaocha.hooks/pre-run
         (fn [test-plan]
           (dbg test-plan) )}))
