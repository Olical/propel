(ns propel.spec-test
  (:require [clojure.test :as t]
            [propel.spec :as spec]))

(t/deftest validate!
  (t/testing "good data is silent"
    (t/is (nil? (spec/validate! number? 10 "ohno"))))

  (t/testing "bad data throws with an explanation"
    (let [{:keys [human computer]}
          (try
            (spec/validate! number? "10" "ohno")
            (catch IllegalArgumentException e
              (ex-data (.getCause e))))]
      (t/is (= "-- Spec failed --------------------\n\n  \"10\"\n\nshould satisfy\n\n  number?\n\n-------------------------\nDetected 1 error\n"
               human))
      (t/is (= "10" (:clojure.spec.alpha/value computer))))))
