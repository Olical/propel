(ns propel.core-test
  (:require [clojure.test :as t]
            [propel.core :as core]
            [propel.util :as util]))

(t/deftest enrich-opts
  (binding [util/free-port (constantly 5555)
            util/unique-name (constantly "foo")]
    (let [f #'core/enrich-opts]
      (t/testing "empty yields defaults"
        (t/is (= {:name "foo"
                  :address "127.0.0.1"
                  :port-file? false
                  :port-file-name ".prepl-port"
                  :env :jvm
                  :args []
                  :accept 'clojure.core.server/io-prepl
                  :port 5555}
                 (f {}))))

      (t/testing "port-file-name enables port-file?"
        (t/is (= {:port-file-name "foo", :port-file? true}
                 (select-keys (f {:port-file-name "foo"})
                              #{:port-file-name :port-file?})))
        (t/is (= {:port-file-name "foo", :port-file? false}
                 (select-keys (f {:port-file-name "foo", :port-file? false})
                              #{:port-file-name :port-file?}))))

      (t/testing "node and browser set args"
        (t/is (= [{:env-opts {:server-name "foo", :port 5555}}]
                 (:args (f {:env :node}))))
        (t/is (= [{:env-opts {:server-name "foo", :port 5555}}]
                 (:args (f {:env :browser})))))

      (t/testing "accept symbol appears appropriate"
        (t/is (= 'cljs.server.node/prepl (:accept (f {:env :node}))))
        (t/is (= 'cljs.server.browser/prepl (:accept (f {:env :browser})))))

      (t/testing "port can be set"
        (t/is (= 6666 (:port (f {:port 6666})))))

      (t/testing "figwheel is special"
        (t/is (= {:figwheel-build "propel", :figwheel-opts {:mode :serve}}
                 (select-keys (f {:env :figwheel})
                              #{:figwheel-build :figwheel-opts}))))

      (t/testing "unknown values flow through"
        (t/is (= :foo (:special (f {:special :foo}))))))))
