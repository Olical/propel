(ns propel.util
  "Useful things that don't conceptually belong to one namespace."
  (:import [java.net ServerSocket]))

(defn log [& msg]
  (apply println "[Propel]" msg))

(defn die [& msg]
  (binding [*out* *err*]
    (log "Error:" (apply str msg)))
  (System/exit 1))

(def ^:private alias->ns
  '{exp expound.alpha
    cljs cljs.repl
    fig figwheel.main.api
    node cljs.server.node
    browser cljs.server.browser})

(defn lapply
  "Require the namespace of the symbol then apply the var with the args."
  [sym & args]
  (let [ns-sym (as-> (symbol (namespace sym)) ns-sym
                 (get alias->ns ns-sym ns-sym))]
    (require ns-sym)
    (apply (resolve (symbol (name ns-sym) (name sym))) args)))

(defn free-port
  "Find a free port we can bind to."
  []
  (let [socket (ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

