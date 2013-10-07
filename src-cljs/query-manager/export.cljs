(ns query-manager.export
  (:require [query-manager.protocols :as proto]))

(defn- on-export!
  []
  (fn [mbus msg]
    (set! (.-location js/window) "/qman/queries/download")))

;;-----------------------------------------------------------------------------
;; Public
;;-----------------------------------------------------------------------------

(defn init!
  [mbus]
  (proto/subscribe! mbus :export-queries (on-export!)))
