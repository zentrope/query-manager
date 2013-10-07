(ns query-manager.proc
  (:require [query-manager.utils :as utils]
            [query-manager.protocols :as proto]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- pull-jobs!
  [channel]
  (proto/publish! channel :jobs-poke {}))

(defn- pull-queries!
  [channel]
  (proto/publish! channel :queries-poke {}))

(defn- shout-clock!
  [channel]
  (proto/publish! channel :clock {:value (.getTime (js/Date.))}))


(defn- start-proc
  [channel interval proc]
  (utils/spawn-after! interval (fn []
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
