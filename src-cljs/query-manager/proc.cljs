(ns query-manager.proc
  (:require [query-manager.utils :refer [spawn-after!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- pull-jobs!
  [channel]
  (channel [:jobs-poke {}]))

(defn- pull-queries!
  [channel]
  (channel [:queries-poke {}]))

(defn- shout-clock!
  [channel]
  (channel [:clock {:value (.getTime (js/Date.))}]))


(defn- start-proc
  [channel interval proc]
  (spawn-after! interval (fn []
                           (proc channel)
                           (start-proc channel interval proc))))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn start
  [channel]
  (start-proc channel 1000 shout-clock!)
  (start-proc channel 2000 pull-jobs!)
  (start-proc channel 2000 pull-queries!))