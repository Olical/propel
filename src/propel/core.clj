(ns propel.core
  "Tools to start prepl servers in various configurations."
  (:require [clojure.core.server :as server]
            [clojure.spec.alpha :as s]
            [expound.alpha :as exp]
            #_[figwheel.main.api :as fig])
  (:import [java.net ServerSocket]))

(defn- free-port
  "Find a free port we can bind to."
  []
  (let [socket (ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

;; Eventual figwheel code...
; (defn -main []
;   (figwheel.main.api/start
;     {:id "dev"
;      :options {:main 'pfig.test}
;      :config {:watch-dirs ["src"]
;               :mode :serve}})

;   (println "=== START PREPL")
;   (server/start-server {:accept 'cljs.core.server/io-prepl
;                         :address "127.0.0.1"
;                         :port 6776
;                         :name "pfig"
;                         :args [:repl-env (fig/repl-env "dev")]})

;   (fig/cljs-repl "dev"))

(s/def ::env #{:jvm :node :rhino :browser :graaljs :nashorn})
(s/def ::port integer?)
(s/def ::address string?)
(s/def ::port-file? boolean?)
(s/def ::port-file-name string?)
(s/def ::opts
  (s/keys :opt-un [::env ::port ::address ::port-file? ::port-file-name]))

(defn- enrich-opts
  "Assign default values and infer configuration for starting a prepl."
  [{:keys [address port-file-name env port]
    :as opts}]
  (let [env (or env :jvm)]
    (merge opts
           {:address (or address "127.0.0.1")
            :port (or port (free-port))
            :port-file-name (or port-file-name ".prepl-port")
            :env env
            :accept (case env
                      :jvm 'clojure.core.server/io-prepl
                      :node 'cljs.server.node/prepl
                      :rhino 'cljs.server.rhino/prepl
                      :browser 'cljs.server.browser/prepl
                      :graaljs 'cljs.server.graaljs/prepl
                      :nashorn 'cljs.server.nashorn/prepl)
            :name (str (gensym "propel-server-"))})))

(defn start-prepl!
  "Start a prepl server."
  [opts]

  (when-not (s/valid? ::opts opts)
    (throw (IllegalArgumentException.
             (ex-info "Failed to start-prepl, invalid arguments."
                      {:human (exp/expound-str ::opts opts)
                       :computer (s/explain-data ::opts opts)}))))

  (let [{:keys [port-file? port-file-name] :as opts} (enrich-opts opts)]
    (when port-file?
      (spit port-file-name (:port opts)))
    (doto opts (server/start-server))))

(comment
  (start-prepl! {})
  (server/stop-servers))
