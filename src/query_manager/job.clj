(ns query-manager.job
  "Experimental at this point. Beware!"
  (:refer-clojure :exclude [reset!])
  (:import [java.util.concurrent Executors ThreadFactory]
           [java.util.concurrent.atomic AtomicLong])
  (:require [clojure.tools.logging :refer [info error]]
            [clojure.java.jdbc :as jdbc]))

;;-----------------------------------------------------------------------------
;; Thread machinery
;;-----------------------------------------------------------------------------

(let [id-counter (AtomicLong.)]
  (defn- id-gen
    []
    (.getAndIncrement id-counter)))

(defn- mk-err-handler
  []
  (proxy [Thread$UncaughtExceptionHandler] []
    (uncaughtException [thread exception]
      (error "In thread" (.getName thread) ":" (.getMessage exception)))))

(defn- mk-thread-factory
  []
  (proxy [ThreadFactory] []
    (newThread [r]
      (doto (Thread. r)
        (.setUncaughtExceptionHandler (mk-err-handler))
        (.setDaemon true) ;; die on JVM shutdown
        (.setName (str "job-proc-" (id-gen)))))))

(defn- mk-thread-pool
  [size]
  (Executors/newFixedThreadPool size (mk-thread-factory)))

;;-----------------------------------------------------------------------------
;; Job Machinery
;;-----------------------------------------------------------------------------

(defn- now
  []
  (System/currentTimeMillis))

(let [id (AtomicLong.)]
  (defn- job-id
    []
    (.getAndIncrement id)))

(defn- mk-job
  [query]
  {:id (job-id)
   :started (now)
   :stopped -1
   :query query
   :status :pending
   :results []})

(defn- mk-runner
  [db jobs job]
  (fn []
    (info " - job start:" (:desc (:query job)))
    (when (:test job)
      (Thread/sleep 2000))
    (try
      (let [sql (:sql (:query job))
            results (doall (jdbc/query db [sql]))
            update (assoc job :status :done :results results :stopped (now))]
        (swap! jobs assoc (:id job) update))
      (catch Throwable t
        (let [update (assoc job :status :failed :stopped (now) :error (str t))]
          (swap! jobs assoc (:id job) update)))
      (finally
        (info " - job complete [" (:desc (:query job)) "]")))))

;;-----------------------------------------------------------------------------
;; Public API
;;-----------------------------------------------------------------------------

(defn mk-jobs
  [db pool-size]
  {:pool (mk-thread-pool pool-size)
   :pool-size pool-size
   :db db
   :jobs (atom {})})

(defn create
  [jobs query]
  (let [new-job (mk-job query)
        runner (mk-runner (:db jobs) (:jobs jobs) new-job)]
    (swap! (:jobs jobs) assoc (:id new-job) new-job)
    (.submit (:pool jobs) runner)
    new-job))

(defn all
  [jobs]
  (vals (deref (:jobs jobs))))

(defn one
  [jobs id]
  (get (deref (:jobs jobs)) id))

(defn delete!
  [jobs id]
  (swap! (:jobs jobs) dissoc id))

(defn reset!
  [jobs]
  (.shutdownNow (:pool jobs))
  (mk-jobs (:db jobs) (:pool-size jobs)))
