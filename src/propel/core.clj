(ns propel.core
  "Tools to start prepl servers in various configurations."
  (:require [clojure.main :as clojure]
            [clojure.spec.alpha :as s]
            [clojure.core.server :as server])
  (:import [java.net ServerSocket]))

(s/def ::env #{:jvm :node :browser :figwheel :rhino :graaljs :nashorn})
(s/def ::port integer?)
(s/def ::address string?)
(s/def ::port-file? boolean?)
(s/def ::port-file-name string?)
(s/def ::name string?)
(s/def ::accept symbol?)
(s/def ::args vector?)
(s/def ::figwheel-build string?)
(s/def ::figwheel-opts map?)
(s/def ::opts
  (s/keys :opt-un [::env ::port ::address ::port-file? ::port-file-name
                   ::name ::accept ::args ::figwheel-build ::figwheel-opts]))

(defn log [& msg]
  (apply println "[Propel]" msg))

(def ^:private alias->ns
  '{exp expound.alpha
    cljs cljs.repl
    fig figwheel.main.api
    node cljs.server.node
    browser cljs.server.browser})

(defn- lapply
  "Require the namespace of the symbol then apply the var with the args."
  [sym & args]
  (let [ns-sym (as-> (symbol (namespace sym)) ns-sym
                 (get alias->ns ns-sym ns-sym))]
    (require ns-sym)
    (apply (resolve (symbol (name ns-sym) (name sym))) args)))

(defn- validate!
  "Validate some data against a spec, throw with message when invalid."
  [spec x msg]
  (when-not (s/valid? spec x)
    (throw (IllegalArgumentException.
             (ex-info msg
                      {:human (lapply 'exp/expound-str spec x)
                       :computer (s/explain-data spec x)})))))

(defn- free-port
  "Find a free port we can bind to."
  []
  (let [socket (ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn- enrich-opts
  "Assign default values and infer configuration for starting a prepl."
  [{:keys [env port] :as opts}]
  (let [env (or env :jvm)
        server-name (str (gensym "propel-server-"))]
    (merge {:name server-name
            :address "127.0.0.1"
            :port-file? false
            :port-file-name ".prepl-port"
            :env env
            :args (if (contains? #{:node :browser} env)
                    ;; Prevents port conflicts and used to find the repl-env later.
                    [{:env-opts {:server-name server-name, :port (free-port)}}]
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
             {:port (free-port)})

           (when (= env :figwheel)
             {:figwheel-build "propel"
              :figwheel-opts {:mode :serve}})

           opts)))

(defn start-prepl!
  "Start a prepl server."
  [opts]
  (validate! ::opts opts "Failed to start-prepl, provided invalid arguments.")

  (let [{:keys [env port-file? port-file-name figwheel-build figwheel-opts]
         :as opts} (enrich-opts opts)
        figwheel? (= env :figwheel)]

    (validate! ::opts opts "Failed to start-prepl, internal configuration error.")

    (when port-file?
      (spit port-file-name (:port opts)))

    (when figwheel?
      (lapply 'fig/start figwheel-opts figwheel-build))

    (server/start-server
      (cond-> opts
        figwheel? (update :args into
                          ;; This can't be done in enrich-opts because the server needs to be started first.
                          [:repl-env (lapply 'fig/repl-env figwheel-build)])))

    opts))

(defn repl
  "Starts a REPL connected to your selected environment."
  [{:keys [env figwheel-build args]}]
  (case env
    :jvm (clojure/main)
    :figwheel (lapply 'fig/cljs-repl figwheel-build)

    ;; This is pretty magic, could probably be less magic.
    ;; The whole world of connecting REPLs to ClojureScript environments is pretty...
    ;; Uh... yeah, it's interesting. I'm surprised I even got some of them working really.
    (:node :browser)
    (lapply 'cljs/repl
            (first (lapply (symbol (name env) "get-envs")
                           (:env-opts (first args)))))

    (do
      (log "No REPL configured for env" env "falling back to regular Clojure JVM REPL.")
      (clojure/main))))
