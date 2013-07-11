(ns query-manager.view.export)

;;-----------------------------------------------------------------------------

(defn- on-export!
  [broadcast]
  (set! (.-location js/window) "/qman/queries/download"))

;;-----------------------------------------------------------------------------
;; Public
;;-----------------------------------------------------------------------------

(defn events
  []
  [:export-queries])

(defn recv
  [broadcast [topic event]]
  (case topic
    :export-queries (on-export! broadcast)
    true))
