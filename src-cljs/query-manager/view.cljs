(ns query-manager.view
  (:require [query-manager.protocols :refer [IView subscribe!]]))

(defrecord View [mbus template-fn]
  IView
  (dom [this]
    (template-fn mbus)))

(defn mk-view
  [mbus template-fn subscriptions]
  (doseq [[topic handler] subscriptions]
    (subscribe! mbus topic handler))
  (View. mbus template-fn))
