(ns query-manager.events
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
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

;;-----------------------------------------------------------------------------
;; Event Handlers
;;-----------------------------------------------------------------------------

(defn- do-app-init!
  [output-q]
  (broadcast! output-q [:job-change (state/all-jobs)])
  (broadcast! output-q [:query-change (state/all-queries)]))

(defn- do-database-get!
  [output-q]
  (broadcast! output-q [:db-change (state/get-db)]))

(defn- do-database-test!
  [output-q db-spec]
  (let [result (state/test-conn! (state/db-specialize db-spec))]
    (broadcast! output-q [:db-test-result result])))

(defn- do-database-save!
  [output-q db-spec]
  (state/put-db! db-spec)
  (state/save-database! (state/get-db))
  (broadcast! output-q [:db-change (state/get-db)]))

(defn- do-job-list!
  [output-q]
  (broadcast! output-q [:job-change (state/all-jobs)]))

(defn- do-job-runall!
  [input-q output-q]
  (doseq [query (state/all-queries)]
    (state/create-job! (state/db-spec) query input-q))
  (broadcast! output-q [:job-change (state/all-jobs)]))

(defn- do-job-run!
  [input-q output-q job]
  (state/create-job! (state/db-spec) (state/one-query job) input-q)
  (broadcast! output-q [:job-change (state/all-jobs)]))

(defn- do-archive!
  [output-q]
  (let [name (state/archive)]
    (broadcast! output-q [:archive-created name])))

(defn- process!
  [input-q output-q [topic msg]]
  ;;
  ;; TODO: Move each of these handlers to a separate functions so that
  ;;       it's easier to get the gist of the topics.
  ;;
  (case topic

    :archive-state (do-archive! output-q)
    :app-init      (do-app-init! output-q)

    :db-get        (do-database-get! output-q)
    :db-test       (do-database-test! output-q msg)
    :db-save       (do-database-save! output-q msg)

    :job-list      (do-job-list! output-q)
    :job-run-all   (do-job-runall! input-q output-q)
    :job-run       (do-job-run! input-q output-q msg)


    :job-get
    (broadcast! output-q [:job-get (state/one-job msg)])

    :job-delete
    (do (state/delete-job! (str msg))
        (broadcast! output-q [:job-change (state/all-jobs)]))

    :job-delete-all
    (do (state/delete-all-jobs!)
        (broadcast! output-q [:job-change (state/all-jobs)]))

    :job-complete
    (broadcast! output-q [:job-change (state/all-jobs)])

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

    :query-create-all
    (do (doseq [q msg]
          (let [{:keys [sql description]} q]
            (state/create-query! sql description)))
        (broadcast! output-q [:query-change (state/all-queries)]))

    :query-delete
    (do (state/delete-query! msg)
        (broadcast! output-q [:query-change (state/all-queries)]))

    :noop))

(defn- event-loop!
  [input-q output-q]
  (async/go-loop []
    (when-let [msg (async/<! input-q)]
      (log-event! "recv:" msg)
      (process! input-q output-q msg)
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
  []
  (atom {:input-q (async/chan)
         :output-q (async/chan)}))

(defn start!
  "Start the event manager."
  [this]
  (log/info "Starting event manager.")
  (event-loop! (:input-q @this) (:output-q @this)))

(defn stop!
  "Stop the event manager. Note: closes all put/get queues."
  [this]
  (log/info "Stopping event manager.")
  (async/close! (:input-q @this) (:output-q @this))
  (reset! this (make (:job-state @this))))
