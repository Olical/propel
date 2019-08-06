(ns propel.core
  "Tools to start prepl servers in various configurations."
  (:require [clojure.core.server :as server]
            [propel.util :as util]))

;; TODO Tidy up how defaults work.
;; TODO Wrap functions in spec with expound printing.

(defn prepl
  "Start a prepl server."
  [{:keys [port address env port-file? port-file-name] :as opts}]
  (let [env (or env :jvm)
        port-file-name (or port-file-name ".prepl-port")
        opts (merge opts
                    {:env env
                     :accept (case env
                               :jvm 'clojure.core.server/io-prepl
                               :node 'cljs.server.node/prepl
                               :rhino 'cljs.server.rhino/prepl
                               :browser 'cljs.server.browser/prepl
                               :graaljs 'cljs.server.graaljs/prepl
                               :nashorn 'cljs.server.nashorn/prepl)
                     :name (str (gensym "propel-server-"))
                     :port (or port (util/free-port))
                     :address (or address "127.0.0.1")
                     :port-file-name port-file-name})]

    (when port-file?
      (spit port-file-name (:port opts)))

    (doto opts (server/start-server))))

(comment
  (prepl {:env :jvm, :port 6666, :port-file? true})
  (server/stop-servers))
