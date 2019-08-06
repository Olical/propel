(ns propel.util
  "Useful functions shared by any propel namespace."
  (:import [java.net ServerSocket]))

(defn free-port
  "Find a free port we can bind to."
  []
  (let [socket (ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

(defn noop
  "A function that does nothing."
  []
  nil)
