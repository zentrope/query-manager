(ns query-manager.main
  (:use-macros [cljs.core.async.macros :only [go-loop]])
  (:require [cljs.core.async :as async]
            [query-manager.view :as view]
            [query-manager.net :as net]))

;;-----------------------------------------------------------------------------

(defn- retrieve-db-info!
  [state queue]
  (net/poke-db queue)
  state)

(defn- save-database-params!
  [state queue data]
  (view/hide-db-form!)
  (net/save-db queue data)
  (assoc state :db-acquired? true))

(defn- database-connection-changed
  [state queue data]
  (cond
    (and (:db-acquired? state)
         (:updated data))
    (do (view/set-frame-db! (:value data))
        (view/fill-db-form! (:value data))
        state)
    (and (not (:db-acquired? state))
         (:updated data))
    (do (view/set-frame-db! (:value data))
        (assoc state :db-acquired? true))
    (not (:db-acquired? state))
    (do (view/show-db-form! queue)
        (view/fill-db-form! data)
        state)
    :else state))

(defn- transition-to-database-form
  [state queue]
  (view/show-db-form! queue)
  (net/poke-db queue)
  state)

(defn- transition-from-database-form
  [state]
  (view/hide-db-form!)
  state)

(defn- test-database!
  [state queue data]
  (net/test-db queue data)
  state)

(defn- on-database-test-result!
  [state result]
  (view/test-db-form! result)
  state)

;;-----------------------------------------------------------------------------

(defn- do-process!
  [state queue [topic data]]
  (case topic
    :db-poke (retrieve-db-info! state queue)
    :db-test (test-database! state queue data)
    :db-test-result (on-database-test-result! state data)
    :db-form-show (transition-to-database-form state queue)
    :db-form-hide (transition-from-database-form state)
    :db-save (save-database-params! state queue data)
    :db-change (database-connection-changed state queue data)
    state))

(defn- process!
  [state queue data]
  (try
    (do-process! state queue data)
    (catch js/Error e
      (.log js/console "ERROR:" (str e))
      state)))

(defn- application-loop!
  [initial-state queue]
  (go-loop [state initial-state]
    (when-let [msg (async/<! queue)]
      (.log js/console "main:" (str msg))
      (let [new-state (process! state queue msg)]
        (recur new-state)))
    :done))

;;-----------------------------------------------------------------------------

(defn main
  []
  (.log js/console ":initializing")
  (let [queue (async/chan)
        state {:db-acquired? false}]
    (view/show-app-frame! queue)
    (application-loop! state queue)
    (async/put! queue [:db-poke {}]))
  (.log js/console ":initialization-complete"))

;;-----------------------------------------------------------------------------

(set! (.-onload js/window) main)
