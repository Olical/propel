(ns propel.main
  "CLI for starting prepl servers and dropping into a REPL."
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.tools.cli :as cli]
            [clojure.edn :as edn]
            [propel.core :as propel]
            [propel.util :as util]))

(def ^:private cli-options
  "Options for use with clojure.tools.cli."
  [["-p" "--port PORT" "Port number, defaults to random open port"
    :parse-fn #(Long/parseUnsignedLong %)]
   ["-a" "--address ADDRESS" "Address for the server, defaults to 127.0.0.1"]
   ["-f" "--port-file-name FILE" "File to write the port to, defaults to .prepl-port"]
   ["-w" "--[no-]write-port-file" "Write the port file? Use of --port-file-name implies true, defaults to false"]
   ["-e" "--env ENV" "What REPL to start ([jvm], node, browser, figwheel, lein-figwheel, rhino, graaljs or nashorn)"
    :parse-fn (fn [s] (-> s (str/replace #"^:" "") (str/lower-case) (keyword)))]
   ["-b" "--figwheel-build BUILD" "Build to use when using the figwheel env (not lein-fighweel), defaults to propel"]
   ["-x" "--extra EDN" "Extra options map you want merged in, you can get creative with this one"]
   ["-h" "--help" "Print this help"]])

(defn- parse-opts
  "Parse command line arguments and return opts usable with propel/start-prepl!"
  [args]
  (-> (cli/parse-opts args cli-options)
      (set/rename-keys {:options :opts})
      (update :opts (fn [{:keys [extra] :as opts}]
                      (-> opts
                          (set/rename-keys {:write-port-file :port-file?})
                          (cond->
                            extra (-> (merge (edn/read-string extra))
                                      (dissoc :extra))))))))

(defn -main
  "Allows you to easily start a single prepl then drop into a rebel-readline REPL."
  [& args]
  (let [{:keys [opts summary]} (parse-opts args)]
    (if (:help opts)
      (println summary)
      (let [{:keys [address env port port-file? port-file-name] :as opts}
            (try
              (propel/start-prepl! opts)
              (catch IllegalArgumentException e
                (let [cause (.getCause e)]
                  (util/die
                    (str (.getMessage cause) "\n\n")
                    (:human (ex-data cause))))))]

        (util/log "Started a" env "prepl at"
                  (str address ":" port
                       (when port-file?
                         (str " (written to \"" port-file-name "\")"))))

        (propel/repl opts)))))
