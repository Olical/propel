(ns propel.util-test
  (:require [clojure.test :as t]
            [propel.util :as util]))

(t/deftest log
  (t/testing "prints with a prefix"
    (t/is (= "[Propel] Hello, World!\n"
             (with-out-str
               (util/log "Hello," "World!"))))))

(t/deftest unique-name
  (t/testing "generates unique names"
    (t/is (not= (util/unique-name "foo") (util/unique-name "foo")))
    (t/is (string? (util/unique-name "foo")))))
