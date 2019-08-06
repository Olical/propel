(ns propel.main
  "CLI for starting prepl servers and dropping into a REPL."
  (:require [clojure.main :as clojure]
            [rebel-readline.core :as rebel]
            [rebel-readline.clojure.main :as rebel-clojure]
            [rebel-readline.clojure.line-reader :as rebel-line-reader]
            [rebel-readline.clojure.service.local :as rebel-local-service]
            [propel.core :as propel]
            [propel.util :as util]))

;; TODO Add CLI argument parsing.
;; TODO Multiple prepls in one go?

(defn -main []
  (println "Clojure" (clojure-version))

  (let [{:keys [address env port port-file? port-file-name]}
        (propel/prepl {:port-file? true})]

    (println "Started" env "prepl at" (str address ":" port))

    (when port-file?
      (println "Port written to" port-file-name)))

  (rebel/with-line-reader
    (rebel-line-reader/create
      (rebel-local-service/create))
    (clojure/repl
      :prompt util/noop
      :read (rebel-clojure/create-repl-read))))
