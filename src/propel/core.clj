(ns propel.core
  "Tools to start prepl servers in various configurations."
  (:require [clojure.core.server :as server]
            [clojure.spec.alpha :as s]
            [clojure.main :as clojure])
  (:import [java.net ServerSocket]))

(s/def ::env #{:jvm :node :rhino :browser :graaljs :nashorn :figwheel})
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
                   ::name ::accept ::args ::figwheel-build]))

(defn- lapply
  "Require the namespace of the symbol then apply the var with the args."
  [sym & args]
  (require (symbol (namespace sym)))
  (apply (resolve sym) args))

(defn- validate!
  "Validate some data against a spec, throw with message when invalid."
  [spec x msg]
  (when-not (s/valid? spec x)
    (throw (IllegalArgumentException.
             (ex-info msg
                      {:human (lapply 'expound.alpha/expound-str spec x)
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
  (let [env (or env :jvm)]
    (merge {:name (str (gensym "propel-server-"))
            :address "127.0.0.1"
            :port-file? false
            :port-file-name ".prepl-port"
            :env env
            :args []
            :accept (case env
                      :jvm 'clojure.core.server/io-prepl
                      :node 'cljs.server.node/prepl
                      :rhino 'cljs.server.rhino/prepl
                      :browser 'cljs.server.browser/prepl
                      :graaljs 'cljs.server.graaljs/prepl
                      :nashorn 'cljs.server.nashorn/prepl
                      :figwheel 'cljs.core.server/io-prepl)}
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
      (lapply 'figwheel.main.api/start figwheel-opts figwheel-build))

    (server/start-server
      (cond-> opts
        figwheel? (update :args into
                          [:repl-env (lapply 'figwheel.api.main/repl-env
                                             figwheel-build)])))

    opts))

(defn repl
  "Starts a REPL connected to your selected environment."
  [{:keys [env figwheel-build]
    server-name :name}]
  (case env
    :jvm (clojure/main)
    :figwheel (lapply 'figwheel.api.main/cljs-repl figwheel-build)

    ;; TODO Rest of the envs.
    :node (lapply 'cljs.repl/repl
                  (first (lapply 'cljs.server.node/get-envs
                                 {:server-name server-name
                                  :port (free-port)})))))
