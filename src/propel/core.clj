(ns propel.core
  "Tools to start prepl servers in various configurations."
  (:require [clojure.main :as clojure]
            [clojure.core.server :as server]
            [propel.spec :as spec]
            [propel.util :as util]))

(defn- enrich-opts
  "Assign default values and infer configuration for starting a prepl."
  [{:keys [env port port-file-name] :as opts}]
  (let [env (or env :jvm)
        server-name (str (gensym "propel-server-"))]
    (merge {:name server-name
            :address "127.0.0.1"
            :port-file? (boolean port-file-name)
            :port-file-name ".prepl-port"
            :env env
            :args (if (contains? #{:node :browser} env)
                    ;; Prevents port conflicts and used to find the repl-env later.
                    [{:env-opts {:server-name server-name, :port (util/free-port)}}]
                    [])
            :accept (case env
                      :jvm 'clojure.core.server/io-prepl
                      :node 'cljs.server.node/prepl
                      :browser 'cljs.server.browser/prepl
                      :figwheel 'cljs.core.server/io-prepl
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
         :as opts} (enrich-opts opts)
        figwheel? (= env :figwheel)]

    (spec/validate! ::spec/opts opts "Failed to start-prepl, internal configuration error.")

    (when port-file?
      (spit port-file-name (:port opts)))

    (when figwheel?
      (util/lapply 'fig/start figwheel-opts figwheel-build))

    (server/start-server
      (cond-> opts
        figwheel? (update :args into
                          ;; This can't be done in enrich-opts because the server needs to be started first.
                          [:repl-env (util/lapply 'fig/repl-env figwheel-build)])))

    opts))

(defn repl
  "Starts a REPL connected to your selected environment."
  [{:keys [env figwheel-build args]}]
  (case env
    :jvm (clojure/main)
    :figwheel (util/lapply 'fig/cljs-repl figwheel-build)

    ;; This is pretty magic, could probably be less magic.
    ;; The whole world of connecting REPLs to ClojureScript environments is pretty...
    ;; Uh... yeah, it's interesting. I'm surprised I even got some of them working really.
    (:node :browser)
    (util/lapply 'cljs/repl
                 (first (util/lapply (symbol (name env) "get-envs")
                                     (:env-opts (first args)))))

    (do
      (util/log "No REPL configured for env" env "falling back to regular Clojure JVM REPL.")
      (clojure/main))))
