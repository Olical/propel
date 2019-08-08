(ns propel.spec
  "For specs and spec related tools."
  (:require [clojure.spec.alpha :as s]
            [propel.util :as util]))

(defn validate!
  "Validate some data against a spec, throw with message when invalid."
  [spec x msg]
  (when-not (s/valid? spec x)
    (throw (IllegalArgumentException.
             (ex-info msg
                      {:human (util/lapply 'exp/expound-str spec x)
                       :computer (s/explain-data spec x)})))))

(s/def ::env #{:jvm :node :browser :figwheel :rhino :graaljs :nashorn})
(s/def ::port integer?)
(s/def ::address string?)
(s/def ::port-file? boolean?)
(s/def ::port-file-name string?)
(s/def ::name string?)
(s/def ::accept symbol?)
(s/def ::args vector?)
(s/def ::figwheel-build string?)
(s/def ::figwheel-opts map?)
(s/def ::opts
  (s/keys :opt-un [::env ::port ::address ::port-file? ::port-file-name
                   ::name ::accept ::args ::figwheel-build ::figwheel-opts]))
