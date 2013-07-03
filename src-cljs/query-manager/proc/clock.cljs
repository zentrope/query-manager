(ns query-manager.proc.clock)

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- start-clock
  [broadcast]
  (js/setTimeout (fn []
                   (broadcast [:clock {:value (.getTime (js/Date.))}])
                   (start-clock broadcast)) 1000))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn start
  [broadcast]
  (start-clock broadcast))
