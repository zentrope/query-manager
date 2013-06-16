(ns queryizer.job
  "Just a stub for now.")

(def ^:private jobs (atom {}))

(defn- make-id
  "Makes a UUID for uniquely identifying jobs."
  []
  (str (java.util.UUID/randomUUID)))

(defn- run-query
  "Runs the query and associates a new status and results with the job
   map."
  [job]
  (assoc job :status :done :results [{:error "not implemented"}]))

(defn- update-jobs
  "Updates our job state with a job."
  [job]
  (swap! jobs (fn [js] (assoc js (:job job) job))))

(defn- run-job
  "Runs the job in a thread."
  [job]
  (doto (Thread. (fn [] (update-jobs (run-query job))))
    (.setName (str "job-runner-" (:job job)))
    (.start)))

(defn- long-run-job
  "Simulates a long running job"
  [job]
  (Thread/sleep 30000)
  (run-job job))

;;-----------------------------------------------------------------------------
;; Public API
;;-----------------------------------------------------------------------------

(defn submit-job
  "Creates a job for query, returns it, spawns a background process to
   actually return the job."
  [query]
  (let [id (make-id)
        job {:job id :query query :status :in-progress :results []}]
    (swap! jobs assoc id job)
    (run-job job)
    job))

(defn long-submit-job
  "Simulates a long running job"
  [query]
  (let [id (make-id)
        job {:job id :query query :status :in-progress :results []}]
    (swap! jobs assoc id job)
    (long-run-job job)
    job))

(defn delete-job
  "Delete an existing job."
  [id]
  (swap! jobs dissoc id))

(defn get-job
  [id]
  "Get the latest about a specific job."
  (get @jobs id))

(defn list-jobs
  "Get a list of all the jobs."
  []
  (vals @jobs))
