(ns propel.main
  "CLI for starting prepl servers and dropping into a REPL."
  (:require [propel.core :as propel]
            [propel.util :as util]))

;; TODO Add CLI argument parsing.

(defn -main
  "Allows you to easily start a single prepl then drop into a rebel-readline REPL."
  []
  (let [{:keys [address env port port-file? port-file-name] :as opts}
        (try
          (propel/start-prepl! {:port-file? true, :env :node})
          (catch IllegalArgumentException e
            (let [cause (.getCause e)]
              (util/die
                (str (.getMessage cause) "\n\n")
                (:human (ex-data cause))))))]

    (util/log "Started a" env "prepl at"
              (str address ":" port
                   (when port-file?
                     (str " (written to \"" port-file-name "\")"))))

    (propel/repl opts)))
