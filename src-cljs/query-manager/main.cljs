(ns query-manager.main
  (:use-macros [cljs.core.async.macros :only [go-loop]])
  (:require [cljs.core.async :as async]
            [query-manager.ajax :refer [ajax]]
            [query-manager.view :as view]))

;;-----------------------------------------------------------------------------

(defn- mread
  [json]
  (let [[topic msg] (js->clj json :keywordize-keys true)]
    [(keyword topic) msg]))

(defn- recv-message
  [queue]
  (ajax :uri "/qman/api/messages"
        :method "GET"
        :on-failure (fn [err] (async/put! queue [:web-error err]))
        :on-success (fn [msg] (async/put! queue (mread msg)))
        :type :json))

(defn- send-message
  [queue msg]
  (ajax :uri "/qman/api/messages"
        :method "POST"
        :on-failure (fn [err] (async/put! queue [:web-error err]))
        :on-success (fn [_] )
        :data msg
        :type :json))

;;-----------------------------------------------------------------------------

(defn- retrieve-db-info!
  [state queue]
  (send-message queue [:db-get {}])
  state)

(defn- save-database-params!
  [state queue db]
  (view/hide-db-form!)
  (send-message queue [:db-save db])
  (assoc state :db-acquired? true))

(defn- database-connection-changed
  [state queue data]
  (cond
    (and (:db-acquired? state) (:updated data))
    (do (view/set-frame-db! data)
        (view/fill-db-form! data)
        (send-message queue [:app-init {}])
        state)
    ;;
    (and (not (:db-acquired? state)) (:updated data))
    (do (view/set-frame-db! data)
        (send-message queue [:app-init {}])
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
  (send-message queue [:db-get {}])
  state)

(defn- transition-from-database-form
  [state]
  (view/hide-db-form!)
  state)

(defn- test-database!
  [state queue db]
  (send-message queue [:db-test db])
  state)

(defn- on-database-test-result!
  [state result]
  (view/test-db-form! result)
  state)

(defn- retrieve-query-info!
  [state queue]
  (send-message queue [:query-list {}])
  state)

(defn- retrieve-jobs-info!
  [state queue]
  (send-message queue [:job-list {}])
  state)

(defn- retrieve-job!
  [state queue job-id]
  (send-message queue [:job-get job-id])
  state)

(defn- on-job-delete!
  [state queue job-id]
  (send-message queue [:job-delete job-id])
  state)

(defn- on-job-delete-all!
  [state queue]
  (send-message queue [:job-delete-all {}])
  state)

(defn- on-jobs-refresh!
  [state queue jobs]
  (view/fill-jobs! queue jobs)
  state)

(defn- on-query-save!
  [state queue query]
  (send-message queue [:query-create query])
  state)

(defn- on-query-run!
  [state queue query-id]
  (send-message queue [:job-run query-id])
  state)

(defn- on-run-all-queries!
  [state queue]
  (send-message queue [:job-run-all {}])
  state)

(defn- on-query-delete!
  [state queue query-id]
  (send-message queue [:query-delete query-id])
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
  (send-message queue [:query-get query-id])
  state)

(defn- on-query-update!
  [state queue query]
  (send-message queue [:query-update query])
  state)

(defn- on-query-export!
  [state]
  (set! (.-location js/window) "/qman/queries/download")
  state)

;;-----------------------------------------------------------------------------

(defn- recv-loop!
  "Long poll the server for incoming events."
  [queue]
  (let [buffer (async/chan)]
    (go-loop []
      (recv-message buffer)
      (when-let [msg (<! buffer)]
        (async/put! queue msg)
        (when (and (= (first msg) :web-error)
                   (= (:status (second msg) 0)))
          (.log js/console "RECV: waiting 5 seconds due to server down.")
          (<! (async/timeout 5000)))
        (recur)))))

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
    :query-run-all (on-run-all-queries! state queue)

    :jobs-poke (retrieve-jobs-info! state queue)
    :job-poke (retrieve-job! state queue data)
    :job-change (on-jobs-refresh! state queue data)
    :job-delete (on-job-delete! state queue data)
    :job-delete-all (on-job-delete-all! state queue)
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

(defn- log-event!
  [[topic data]]
  (let [skips #{:query-change :job-change :queries-poke :jobs-poke}]
    (when (contains? skips topic)
      (let [s (str [topic data])]
        (if (> (count s) 70)
          (.log js/console "main:" (subs s 0 70))
          (.log js/console "main:" s))))
    (when-not (contains? skips topic)
      (.log js/console "main:" (str [topic data])))))

(defn- application-loop!
  [initial-state queue]
  (go-loop [state initial-state]
    (when-let [msg (async/<! queue)]
      (log-event! msg)
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
    (recv-loop! queue)
    (application-loop! state queue)
    (async/put! queue [:db-poke {}]))

  (.log js/console ":initialization-complete"))

;;-----------------------------------------------------------------------------

(set! (.-onload js/window) main)
