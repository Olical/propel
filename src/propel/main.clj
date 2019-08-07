(ns propel.main
  "CLI for starting prepl servers and dropping into a REPL."
  (:require [propel.core :as propel]))

;; TODO Add CLI argument parsing.

(defn- die [& msg]
  (binding [*out* *err*]
    (println "Error:" (apply str msg)))
  (System/exit 1))

(defn -main
  "Allows you to easily start a single prepl then drop into a rebel-readline REPL."
  []
  (println "Clojure" (clojure-version))

  (let [{:keys [address env port port-file? port-file-name]}
        (try
          (propel/start-prepl {:port-file? true})
          (catch IllegalArgumentException e
            (let [cause (.getCause e)]
              (die
                (str (.getMessage cause) "\n\n")
                (:human (ex-data cause))))))]

    (println "Started" env "prepl at" (str address ":" port))

    (when port-file?
      (println "Port written to" port-file-name)))

  (propel/repl))
