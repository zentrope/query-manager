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
    (and (:db-acquired? state) (:updated data))
    (do (view/set-frame-db! data)
        (view/fill-db-form! data)
        state)
    ;;
    (and (not (:db-acquired? state)) (:updated data))
    (do (view/set-frame-db! data)
        (assoc state :db-acquired? true))
    ;;
    (not (:db-acquired? state))
    (do (view/show-db-form! queue)
        (view/fill-db-form! data)
        state)
    ;;
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

(defn- retrieve-query-info!
  [state queue]
  (net/poke-queries queue)
  state)

(defn- retrieve-jobs-info!
  [state queue]
  (net/poke-jobs queue)
  state)

(defn- retrieve-job!
  [state queue job-id]
  (net/poke-job queue job-id)
  state)

(defn- on-job-delete!
  [state queue job-id]
  (net/delete-job queue job-id)
  state)

(defn- on-jobs-refresh!
  [state queue jobs]
  (view/fill-jobs! queue jobs)
  state)

(defn- on-query-save!
  [state queue query]
  (net/save-query queue query)
  state)

(defn- on-query-run!
  [state queue query-id]
  (net/run-job queue query-id)
  state)

(defn- on-query-delete!
  [state queue query-id]
  (net/delete-query queue query-id)
  state)

(defn- on-queries-refresh!
  [state queue queries]
  (view/fill-queries! queue queries)
  state)

(defn- transition->job-viewer!
  [state queue]
  (view/show-job-viewer! queue)
  state)

(defn- transition<-job-viewer!
  [state]
  (view/hide-job-viewer!)
  state)

(defn- on-job-refresh!
  [state job]
  (view/fill-job-viewer! job)
  state)

(defn- transition->query-form!
  [state queue]
  (view/show-query-form! queue)
  state)

(defn- transition<-query-form!
  [state]
  (view/hide-query-form!)
  state)

(defn- on-query-refresh!
  [state query]
  (view/fill-query-form! query)
  state)

(defn- on-query-request!
  [state queue query-id]
  (net/poke-query queue query-id)
  state)

(defn- on-query-update!
  [state queue query]
  (net/update-query queue query)
  state)

(defn- on-query-export!
  [state]
  (set! (.-location js/window) "/qman/queries/download")
  state)

;;-----------------------------------------------------------------------------

(defn- poll-loop!
  [queue topic interval]
  (go-loop []
    (<! (async/timeout interval))
    (async/put! queue [topic {}])
    (recur)))

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

    :queries-poke (retrieve-query-info! state queue)
    :query-save (on-query-save! state queue data)
    :query-change (on-queries-refresh! state queue data)
    :query-run (on-query-run! state queue data)
    :query-delete (on-query-delete! state queue data)
    :query-form-show (transition->query-form! state queue)
    :query-form-hide (transition<-query-form! state)
    :query-get (on-query-refresh! state data)
    :query-poke (on-query-request! state queue data)
    :query-update (on-query-update! state queue data)

    :jobs-poke (retrieve-jobs-info! state queue)
    :job-poke (retrieve-job! state queue data)
    :job-change (on-jobs-refresh! state queue data)
    :job-delete (on-job-delete! state queue data)
    :job-view-show (transition->job-viewer! state queue)
    :job-view-hide (transition<-job-viewer! state)
    :job-get (on-job-refresh! state data)

    :export-queries (on-query-export! state)
    state))

(defn- process!
  [state queue data]
  (try
    (do-process! state queue data)
    (catch js/Error e
      (.log js/console "ERROR:" (str e))
      state)))

(defn- log-event-stream!
  [[topic data]]
  (let [skips #{:query-change :job-change :queries-poke :jobs-poke}]
    (when-not (contains? skips topic)
      (.log js/console "main:" (str [topic data])))))

(defn- application-loop!
  [initial-state queue]
  (go-loop [state initial-state]
    (when-let [msg (async/<! queue)]
      (log-event-stream! msg)
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
    (async/put! queue [:db-poke {}])
    (async/put! queue [:queries-poke {}])
    (async/put! queue [:jobs-poke {}])
    (poll-loop! queue :queries-poke 10000)
    (poll-loop! queue :jobs-poke 1000))
  (.log js/console ":initialization-complete"))

;;-----------------------------------------------------------------------------

(set! (.-onload js/window) main)
