(ns propel.util
  "Useful things that don't conceptually belong to one namespace."
  (:require [clojure.main :as clojure]
            [clojure.pprint :as pprint])
  (:import [java.net ServerSocket]))

(defn log [& msg]
  (apply println "[Propel]" msg))

(defn error [err & msg]
  (binding [*out* *err*]
    (apply log "Error:" msg)
    (-> err
        (cond-> (not (map? err)) (Throwable->map))
        (doto (pprint/pprint))
        (clojure/ex-triage)
        (clojure/ex-str)
        (println))))

(defn die [& msg]
  (binding [*out* *err*]
    (log "Error:" (apply str msg)))
  (System/exit 1))

(def ^:private alias->ns
  '{exp expound.alpha
    cljs cljs.repl
    fig figwheel.main.api
    lfig figwheel-sidecar.repl-api
    node cljs.server.node
    browser cljs.server.browser})

(defn lapply
  "Require the namespace of the symbol then apply the var with the args."
  [sym & args]
  (let [ns-sym (as-> (symbol (namespace sym)) ns-sym
                 (get alias->ns ns-sym ns-sym))]
    (require ns-sym)
    (apply (resolve (symbol (name ns-sym) (name sym))) args)))

(defn ^:dynamic free-port
  "Find a free port we can bind to."
  []
  (let [socket (ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn ^:dynamic unique-name
  "Generates a unique prefixed name string with a label."
  [label]
  (str (gensym (str "propel-" label "-"))))

(defmacro thread
  "Useful helper to run code in a thread but ensure errors are caught and
  logged correctly."
  [use-case & body]
  `(future
     (try
       ~@body
       (catch Throwable t#
         (error t# "From thread" (str "'" ~use-case "'"))))))

(defn write
  "Write the full data to the stream and then flush the stream."
  [stream data]
  (doto stream
    (.write data 0 (count data))
    (.flush)))
