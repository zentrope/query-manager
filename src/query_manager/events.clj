(ns query-manager.events
  (:require [clojure.core.async :as async]))

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
  [client-queues event]
  (log-event! "pub:" event)
  (doseq [q client-queues]
    (async/put! q event)))

(defn- process!
  [{:keys [event-queue job-state db-state file-repo client-queues] :as manager} [topic data]]
  (case topic

    :app-init
    (do (broadcast! client-queues [:job-change (job/all job-state)])
        (broadcast! client-queues [:query-change (sql/all)]))

    :db-get
    (broadcast! client-queues [:db-change (db/get db-state)])

    :db-test
    (let [result (test-db/test-connection (db/specialize msg))]
      (broadcast! client-queues [:db-test-result result]))

    :db-save
    (do (db/put db-state msg)
        (repo/save-database! file-repo (db/get db-state))
        (broadcast! client-queues [:db-change (db/get db-state)]))

    :job-list
    (broadcast! client-queues [:job-change (job/all job-state)])

    :job-run-all
    (do (doseq [query (sql/all)]
          (job/create job-state (db/spec db-state) query control-ch))
        (broadcast! client-queues [:job-change (job/all job-state)]))

    :job-run
    (do (job/create job-state (db/spec db-state) (sql/one msg) control-ch)
        (broadcast! client-queues [:job-change (job/all job-state)]))

    :job-get
    (broadcast! client-queues [:job-get (job/one job-state (Long/parseLong msg))])

    :job-delete
    (do (job/delete! job-state (str msg))
        (broadcast! client-queues [:job-change (job/all job-state)]))

    :job-delete-all
    (do (job/delete-all! job-state)
        (broadcast! client-queues [:job-change (job/all job-state)]))

    :job-complete
    (broadcast! client-queues [:job-change (job/all job-state)])

    :query-get
    (broadcast! client-queues [:query-get (sql/one msg)])

    :query-list
    (broadcast! client-queues [:query-change (sql/all)])

    :query-update
    (if-let [query (sql/one (:id msg))]
      (let [update (merge query msg)]
        (sql/update! update)
        (broadcast! client-queues [:query-change (sql/all)]))
      (async/put! [:http-error 404]))

    :query-create
    (let [{:keys [sql description]} msg]
      (sql/create! sql description)
      (broadcast! client-queues [:query-change (sql/all)]))

    :query-delete
    (do (sql/delete! msg)
        (broadcast! client-queues [:query-change (sql/all)]))

    :noop))

(defn- event-loop!
  [{:keys [event-queue] :as manager}]
  (async/go-loop []
    (when-let [msg (async/<! event-queue)]
      (log-event! "con:" msg)
      (process! manager msg)
      (recur))))

;;-----------------------------------------------------------------------------
;; Service
;;-----------------------------------------------------------------------------

(defn event-queue
  [manager]
  (:event-queue @manager))

(defn manager
  [clients job-state db-state file-repo]
  (atom {:event-queue (async/chan)
         :client-queues (set clients)
         :job-state job-state
         :db-state db-state
         :file-repo file-repo}))

(defn start!
  [manager]
  (event-loop! @manager))

(defn stop!
  [manager]
  (async/close! (:event-queue @manager))
  ;;
  ;; Does this make sense? If other modules have a handle on the old
  ;; queue, they'll never get a handle on the new one. Hm.
  ;;
  (swap! manager assoc :event-queue (async/chan)))
