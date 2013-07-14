(ns query-manager.export)

;;-----------------------------------------------------------------------------

(defn- on-export!
  [broadcast]
  (set! (.-location js/window) "/qman/queries/download"))

;;-----------------------------------------------------------------------------
;; Public
;;-----------------------------------------------------------------------------

(defn topics
  []
  [:export-queries])

(defn recv
  [broadcast [topic event]]
  (case topic
    :export-queries (on-export! broadcast)
    true))
