(ns query-manager.events
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [query-manager.repo :as repo]
            [query-manager.job :as job]
            [query-manager.test-db :as test-db]
            [query-manager.state :as state]))

;;-----------------------------------------------------------------------------
;; Event Processing
;;-----------------------------------------------------------------------------

(defn- log-event!
  [name event]
  (let [s (format "%s" event)]
    (try
      (log/info name (subs s 0 76) "... ]")
      (catch Throwable t
        (log/info name s)))))

(defn- broadcast!
  [output-q event]
  (log-event! "send:" event)
  (async/put! output-q event))

(defn- process!
  [input-q output-q job-state [topic msg]]
  ;;
  ;; TODO: Move each of these handlers to a separate functions so that
  ;;       it's easier to get the gist of the topics.
  ;;
  (case topic

    :app-init
    (do (broadcast! output-q [:job-change (job/all job-state)])
        (broadcast! output-q [:query-change (state/all-queries)]))

    :db-get
    (broadcast! output-q [:db-change (state/get-db)])

    :db-test
    (let [result (test-db/test-connection (state/db-specialize msg))]
      (broadcast! output-q [:db-test-result result]))

    :db-save
    (do (state/put-db! msg)
        (repo/save-database! (state/get-db))
        (broadcast! output-q [:db-change (state/get-db)]))

    :job-list
    (broadcast! output-q [:job-change (job/all job-state)])

    :job-run-all
    (do (doseq [query (state/all-queries)]
          (job/create job-state (state/db-spec) query input-q))
        (broadcast! output-q [:job-change (job/all job-state)]))

    :job-run
    (do (job/create job-state (state/db-spec) (state/one-query msg) input-q)
        (broadcast! output-q [:job-change (job/all job-state)]))

    :job-get
    (broadcast! output-q [:job-get (job/one job-state (Long/parseLong msg))])

    :job-delete
    (do (job/delete! job-state (str msg))
        (broadcast! output-q [:job-change (job/all job-state)]))

    :job-delete-all
    (do (job/delete-all! job-state)
        (broadcast! output-q [:job-change (job/all job-state)]))

    :job-complete
    (broadcast! output-q [:job-change (job/all job-state)])

    :query-get
    (broadcast! output-q [:query-get (state/one-query msg)])

    :query-list
    (broadcast! output-q [:query-change (state/all-queries)])

    :query-update
    (if-let [query (state/one-query (:id msg))]
      (let [update (merge query msg)]
        (state/update-query! update)
        (broadcast! output-q [:query-change (state/all-queries)]))
      (broadcast! output-q [:http-error 404]))

    :query-create
    (let [{:keys [sql description]} msg]
      (state/create-query! sql description)
      (broadcast! output-q [:query-change (state/all-queries)]))

    :query-delete
    (do (state/delete-query! msg)
        (broadcast! output-q [:query-change (state/all-queries)]))

    :noop))

(defn- event-loop!
  [input-q output-q job-state]
  (async/go-loop []
    (when-let [msg (async/<! input-q)]
      (log-event! "recv:" msg)
      (process! input-q output-q job-state msg)
      (recur))))

;;-----------------------------------------------------------------------------
;; Service
;;-----------------------------------------------------------------------------

(defn put-event-q
  "Return a queue you can use to send events to the event manager."
  [this]
  (:input-q @this))

(defn get-event-q
  "Return a queue you can use to consume events published by the event
  manager."
  [this]
  (:output-q @this))

(defn make
  "Make a new event manager."
  [job-state]
  (atom {:input-q (async/chan)
         :output-q (async/chan)
         :job-state job-state}))

(defn start!
  "Start the event manager."
  [this]
  (log/info "Starting event manager.")
  (event-loop! (:input-q @this) (:output-q @this) (:job-state @this)))

(defn stop!
  "Stop the event manager. Note: closes all put/get queues."
  [this]
  (log/info "Stopping event manager.")
  (async/close! (:input-q @this) (:output-q @this))
  (reset! this (make (:job-state @this))))
