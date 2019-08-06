(ns propel.main
  "CLI for starting prepl servers and dropping into a REPL."
  (:require [propel.core :as propel]))

;; TODO Add CLI argument parsing.

(defn -main
  "Allows you to easily start a single prepl then drop into a rebel-readline REPL."
  []
  (println "Clojure" (clojure-version))

  (let [{:keys [address env port port-file? port-file-name]}
        (propel/prepl {:port-file? true})]

    (println "Started" env "prepl at" (str address ":" port))

    (when port-file?
      (println "Port written to" port-file-name)))

  (propel/start-rebel-readline))
