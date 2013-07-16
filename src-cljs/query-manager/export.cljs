(ns query-manager.export
  (:require [query-manager.protocols :refer [subscribe!]]))

(defn- on-export!
  []
  (fn [mbus msg]
    (set! (.-location js/window) "/qman/queries/download")))

;;-----------------------------------------------------------------------------
;; Public
;;-----------------------------------------------------------------------------

(defn init!
  [mbus]
  (subscribe! mbus :export-queries (on-export!)))
