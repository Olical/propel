(ns propel.core
  "Tools to start prepl servers in various configurations."
  (:require [clojure.main :as clojure]
            [clojure.core.server :as server]
            [clojure.java.io :as io]
            [clojure.core.async :as a]
            [clojure.pprint :as pprint]
            [clojure.tools.reader :as tr]
            [propel.spec :as spec]
            [propel.util :as util])
  (:import [java.io PipedInputStream PipedOutputStream]))

(defn- enrich-opts
  "Assign default values and infer configuration for starting a prepl."
  [{:keys [env port port-file-name] :as opts}]
  (let [env (or env :jvm)
        server-name (util/unique-name "server")]
    (merge {:name server-name
            :address "127.0.0.1"
            :port-file? (boolean port-file-name)
            :port-file-name ".prepl-port"
            :env env
            :args (if (contains? #{:node :browser} env)
                    ;; Prevents port clashes in ClojureScript prepls.
                    [{:env-opts {:port (util/free-port)}}]
                    [])
            :accept (case env
                      :jvm 'clojure.core.server/io-prepl
                      :node 'cljs.server.node/prepl
                      :browser 'cljs.server.browser/prepl
                      :figwheel 'cljs.core.server/io-prepl
                      :lein-figwheel 'cljs.core.server/io-prepl
                      :rhino 'cljs.server.rhino/prepl
                      :graaljs 'cljs.server.graaljs/prepl
                      :nashorn 'cljs.server.nashorn/prepl)}

           (when-not port
             {:port (util/free-port)})

           (when (= env :figwheel)
             {:figwheel-build "propel"
              :figwheel-opts {:mode :serve}})

           opts)))

(defn start-prepl!
  "Start a prepl server."
  [opts]
  (spec/validate! ::spec/opts opts "Failed to start-prepl, provided invalid arguments.")

  (let [{:keys [env port-file? port-file-name figwheel-build figwheel-opts]
         :as opts} (enrich-opts opts)]

    (spec/validate! ::spec/opts opts "Failed to start-prepl, internal configuration error.")

    (when port-file?
      (spit port-file-name (:port opts))
      (.addShutdownHook (Runtime/getRuntime) (Thread. #(io/delete-file port-file-name true))))

    (when (= env :figwheel)
      (util/lapply 'fig/start figwheel-opts figwheel-build))

    (when (= env :lein-figwheel)
      (util/lapply 'lfig/start-figwheel!))

    (server/start-server
      ;; This can't be done in enrich-opts because the servers needs to be started first.
      (cond-> opts
        (= env :figwheel)
        (update :args into [:repl-env (util/lapply 'fig/repl-env figwheel-build)])

        (= env :lein-figwheel)
        (update :args into [:repl-env (util/lapply 'lfig/repl-env)])))

    opts))

(defn repl
  "Starts a REPL connected to the address and port specified in the opts map."
  [{:keys [address port]}]
  (let [eval-chan (a/chan 32)
        read-chan (a/chan 32)
        ret-chan (a/chan 32)
        input (PipedInputStream.)
        output (PipedOutputStream. input)]

    (util/thread "remote prepl connection"
      (with-open [input-reader (io/reader input)]
        (server/remote-prepl
          address port
          input-reader
          (fn [msg] (a/>!! read-chan msg))
          :valf identity)))

    (util/thread "read loop"
      (loop []
        (when-let [{:keys [tag val] :as msg} (a/<!! read-chan)]
          (case tag
            :out (do
                   (print val)
                   (flush))
            :err (binding [*out* *err*] 
                   (print val)
                   (flush))
            :tap (try
                   (pprint/pprint (read-string val))
                   (catch Throwable _t
                     (println val)))
            :ret (a/>!! ret-chan msg)
            (util/log "Unrecognised prepl response:" (pr-str msg)))
          (recur))))

    (util/thread "eval loop"
      (with-open [output-writer (io/writer output)]
        (loop []
          (when-let [code (a/<!! eval-chan)]
            (util/write output-writer (str code "\n"))
            (recur)))))

    ;; Load REPL tooling functions.
    ;; We fire Clojure and ClojureScript in the hope that one of them sticks.
    ;; Errors are ignored. I would use reader conditionals but old Clojure
    ;; prepl versions don't support them.
    (doseq [code ["(use 'clojure.repl)"
                  "(use '[cljs.repl :only (doc source error->str)])"]]
      (a/>!! eval-chan code)
      (a/<!! ret-chan))

    (clojure/repl
      :read (fn [request-prompt request-exit]
              (or ({:line-start request-prompt :stream-end request-exit}
                   (clojure/skip-whitespace *in*))
                  (let [input (binding [tr/*default-data-reader-fn* tagged-literal
                                        tr/*alias-map* (constantly 'user)]
                                (tr/read {:read-cond :preserve} *in*))]
                    (clojure/skip-if-eol *in*)
                    input)))
      :eval (fn [form]
              (a/>!! eval-chan (pr-str form))
              (a/<!! ret-chan))
      :print (fn [{:keys [val exception]}]
               (if exception
                 (util/error (read-string val) "From remote prepl eval")
                 (println val)))
      :prompt (fn []
                (a/>!! eval-chan ":prompt")
                (print (str (:ns (a/<!! ret-chan)) "=> "))))

    ;; Known issue: If you send :repl/quit the prepl will exit but the local
    ;; REPL will continue to loop. Not super important so I'll leave it for now.

    ;; Ensure the remote prepl exits properly if the user sends EOF
    ;; to their local REPL.
    (a/>!! eval-chan ":repl/quit")
    (run! a/close! [eval-chan read-chan ret-chan])))
