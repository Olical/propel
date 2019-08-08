(ns propel.main-test
  (:require [clojure.test :as t]
            [propel.main :as main]))

(t/deftest parse-opts
  (letfn [(f [& args] (:opts (#'main/parse-opts args)))]
    (t/testing "empty by default"
      (t/is (= {} (f)))
      (t/is (= {} (f " "))))

    (t/testing "port is parsed"
      (t/is (= {:port 5555} (f "-p" "5555"))))

    (t/testing "write-port-file is renamed"
      (t/is (= {:port-file? true} (f "-w" "foo"))))

    (t/testing "extra is parsed"
      (t/is (= {:foo :bar} (f "-x" "{:foo :bar}"))))

    (t/testing "env is parsed to a keyword"
      (t/is (= {:env :jvm} (f "-e" "jvm")))
      (t/is (= {:env :node} (f "-e" ":node")))
      (t/is (= {:env :browser} (f "-e" ":Browser"))))))
