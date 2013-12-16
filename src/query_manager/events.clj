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

;;-----------------------------------------------------------------------------

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

(defn- do-job-get!
  [output-q job-id]
  (broadcast! output-q [:job-get (state/one-job job-id)]))

(defn- do-job-delete!
  [output-q job-id]
  (state/delete-job! (str job-id))
  (broadcast! output-q [:job-change (state/all-jobs)]))

(defn- do-job-delete-all!
  [output-q]
  (state/delete-all-jobs!)
  (broadcast! output-q [:job-change (state/all-jobs)]))

(defn- do-job-complete!
  [output-q]
  (broadcast! output-q [:job-change (state/all-jobs)]))

;;-----------------------------------------------------------------------------

(defn- do-query-get!
  [output-q qid]
  (broadcast! output-q [:query-get (state/one-query qid)]))

(defn- do-query-list!
  [output-q]
  (broadcast! output-q [:query-change (state/all-queries)]))

(defn- do-query-update!
  [output-q new-query]
  (if-let [query (state/one-query (:id new-query))]
    (let [update (merge query new-query)]
      (state/update-query! update)
      (broadcast! output-q [:query-change (state/all-queries)]))
    (broadcast! output-q [:http-error 404])))

(defn- do-query-create!
  [output-q query]
  (let [{:keys [sql description]} query]
    (state/create-query! sql description)
    (broadcast! output-q [:query-change (state/all-queries)])))

(defn- do-query-create-all!
  [output-q queries]
  (doseq [q queries]
    (let [{:keys [sql description]} q]
      (state/create-query! sql description)))
  (broadcast! output-q [:query-change (state/all-queries)]))

(defn- do-query-delete!
  [output-q qid]
  (state/delete-query! qid)
  (broadcast! output-q [:query-change (state/all-queries)]))

;;-----------------------------------------------------------------------------

(defn- do-app-init!
  [output-q]
  (broadcast! output-q [:job-change (state/all-jobs)])
  (broadcast! output-q [:query-change (state/all-queries)]))

(defn- do-archive!
  [output-q]
  (let [name (state/archive)]
    (broadcast! output-q [:archive-created name])))

(defn- do-noop!
  [output-q topic msg]
  (log/info "Unable to process message: " [topic msg]))

;;-----------------------------------------------------------------------------

(defn- process!
  [input-q output-q [topic msg]]
  (case topic
    :db-get           (do-database-get! output-q)
    :db-test          (do-database-test! output-q msg)
    :db-save          (do-database-save! output-q msg)

    :job-list         (do-job-list! output-q)
    :job-run-all      (do-job-runall! input-q output-q)
    :job-run          (do-job-run! input-q output-q msg)
    :job-get          (do-job-get! output-q msg)
    :job-delete       (do-job-delete! output-q msg)
    :job-delete-all   (do-job-delete-all! output-q)
    :job-complete     (do-job-complete! output-q)

    :query-get        (do-query-get! output-q msg)
    :query-list       (do-query-list! output-q)
    :query-update     (do-query-update! output-q msg)
    :query-create     (do-query-create! output-q msg)
    :query-create-all (do-query-create-all! output-q msg)
    :query-delete     (do-query-delete! output-q msg)

    :app-init         (do-app-init! output-q)
    :archive-state    (do-archive! output-q)
    (do-noop! topic msg)))

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
