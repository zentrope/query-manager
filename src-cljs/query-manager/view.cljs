(ns query-manager.view
  (:require [query-manager.protocols :as proto]))

(defrecord View [mbus template-fn]
  proto/IView
  (dom [this]
    (template-fn mbus)))

(defn mk-view
  [mbus template-fn subscriptions]
  (doseq [[topic handler] subscriptions]
    (proto/subscribe! mbus topic handler))
  (View. mbus template-fn))
