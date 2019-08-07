(ns propel.core
  "Tools to start prepl servers in various configurations."
  (:require [clojure.core.server :as server]
            [clojure.main :as clojure]
            [clojure.spec.alpha :as s]
            [expound.alpha :as exp]
            [rebel-readline.core :as rebel]
            [rebel-readline.clojure.main :as rebel-clojure]
            [rebel-readline.clojure.line-reader :as rebel-line-reader]
            [rebel-readline.clojure.service.local :as rebel-local-service]
            #_[figwheel.main.api :as fig])
  (:import [java.net ServerSocket]
           [clojure.lang ExceptionInfo]))

;; TODO Lazy load all ClojureScript stuff.

(defn- free-port
  "Find a free port we can bind to."
  []
  (let [socket (ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn- noop
  "A function that does nothing."
  []
  nil)

(defn repl
  "Start a rebel-readline REPL, falls back to a regular REPL."
  []
  (rebel/with-line-reader
    (rebel-line-reader/create
      (rebel-local-service/create))
    (clojure/repl
      :prompt noop
      :read (rebel-clojure/create-repl-read)))

  (try
    (rebel/with-readline-in
      (rebel-line-reader/create
        (rebel-local-service/create))
      (clojure/repl :prompt noop))
    (catch ExceptionInfo e
      (if (-> e ex-data :type (= :rebel-readline.jline-api/bad-terminal))
        (do
          (println (.getMessage e))
          (clojure/repl))
        (throw e)))))

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
(s/def ::prepl-opts
  (s/keys :opt-un [::env ::port ::address ::port-file? ::port-file-name]))

(defn start-prepl
  "Start a prepl server."
  [{:keys [port address env port-file? port-file-name]
    :or {address "127.0.0.1"
         port-file-name ".prepl-port"
         env :jvm}
    :as opts}]

  (when-not (s/valid? ::prepl-opts opts)
    (throw (IllegalArgumentException.
             (ex-info "Failed to start-prepl, invalid arguments."
                      {:human (exp/expound-str ::prepl-opts opts)
                       :computer (s/explain-data ::prepl-opts opts)}))))

  (let [opts (merge opts
                    {:accept (case env
                               :jvm 'clojure.core.server/io-prepl
                               :node 'cljs.server.node/prepl
                               :rhino 'cljs.server.rhino/prepl
                               :browser 'cljs.server.browser/prepl
                               :graaljs 'cljs.server.graaljs/prepl
                               :nashorn 'cljs.server.nashorn/prepl)
                     :name (str (gensym "propel-server-"))
                     :port (or port (free-port))})]

    (when port-file?
      (spit port-file-name (:port opts)))

    (doto opts (server/start-server))))

(comment
  (start-prepl {})
  (server/stop-servers))
