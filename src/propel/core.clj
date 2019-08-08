(ns propel.core
  "Tools to start prepl servers in various configurations."
  (:require [clojure.core.server :as server]
            [clojure.spec.alpha :as s]
            [clojure.main :as clojure]
            [expound.alpha :as exp]

            ;; TODO Build a generic lazy load fn/macro for this.
            [cljs.repl :as cljs]
            [cljs.server.node :as server-node])
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
(s/def ::opts
  (s/keys :opt-un [::env ::port ::address ::port-file? ::port-file-name
                   ::name ::accept ::args ::figwheel-build]))

(defn- validate!
  "Validate some data against a spec, throw with message when invalid."
  [spec x msg]
  (when-not (s/valid? spec x)
    (throw (IllegalArgumentException.
             (ex-info msg
                      {:human (exp/expound-str spec x)
                       :computer (s/explain-data spec x)})))))

(defn- free-port
  "Find a free port we can bind to."
  []
  (let [socket (ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn- fig
  "Ensure figwheel.main.api is required and execute the function named by the keyword."
  [f-name & args]
  (require 'figwheel.main.api)
  (apply (resolve (symbol "figwheel.main.api" (name f-name))) args))

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
                      :jvm 'server/io-prepl
                      :node 'server-node/prepl
                      :rhino 'cljs.server.rhino/prepl
                      :browser 'cljs.server.browser/prepl
                      :graaljs 'cljs.server.graaljs/prepl
                      :nashorn 'cljs.server.nashorn/prepl
                      :figwheel 'cljs.core.server/io-prepl)}
           (when-not port
             {:port (free-port)})
           (when (= env :figwheel)
             {:figwheel-build "propel"})
           opts)))

(defn start-prepl!
  "Start a prepl server."
  [opts]
  (validate! ::opts opts "Failed to start-prepl, provided invalid arguments.")

  (let [{:keys [env port-file? port-file-name figwheel-build]
         :as opts} (enrich-opts opts)
        figwheel? (= env :figwheel)]

    (validate! ::opts opts "Failed to start-prepl, internal configuration error.")

    (when port-file?
      (spit port-file-name (:port opts)))

    (when figwheel?
      (fig :start {:mode :serve} figwheel-build))

    (server/start-server
      (cond-> opts
        figwheel? (update :args into [:repl-env (fig :repl-env figwheel-build)])))

    opts))

(defn repl
  "Starts a REPL connected to your selected environment."
  [{:keys [env figwheel-build]
    server-name :name}]
  (case env
    :jvm (clojure/main)
    :figwheel (fig :cljs-repl figwheel-build)

    ;; TODO Rest of the envs.
    :node (cljs/repl (first (server-node/get-envs {:server-name server-name
                                                   :port (free-port)})))))

(comment
  (start-prepl! {:env :node :port 6565})
  (server/stop-servers))
