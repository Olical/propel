(ns propel.main
  "CLI for starting prepl servers and dropping into a REPL."
  (:require [propel.core :as propel]))

;; TODO Add CLI argument parsing.

(defn- log [& msg]
  (apply println "[Propel]" msg))

(defn- die [& msg]
  (binding [*out* *err*]
    (log "Error:" (apply str msg)))
  (System/exit 1))

(defn -main
  "Allows you to easily start a single prepl then drop into a rebel-readline REPL."
  []
  (let [{:keys [address env port port-file? port-file-name] :as opts}
        (try
          (propel/start-prepl! {:port-file? true})
          (propel/start-prepl! {:port-file? true
                                :port-file-name ".figwheel-prepl-port"
                                :env :figwheel})
          (catch IllegalArgumentException e
            (let [cause (.getCause e)]
              (die
                (str (.getMessage cause) "\n\n")
                (:human (ex-data cause))))))]

    (log "Started a" env "prepl at"
         (str address ":" port
              (when port-file?
                (str " (written to \"" port-file-name "\")"))))

    (propel/repl opts)))
