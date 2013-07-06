(ns query-manager.proc.job-monitor
  (:require [query-manager.utils :refer [spawn-after!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- start-job-monitor
  [broadcast]
  (spawn-after! 2000 (fn []
                       (broadcast [:jobs-poke {}])
                       (start-job-monitor broadcast))))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn start
  [broadcast]
  (start-job-monitor broadcast))
