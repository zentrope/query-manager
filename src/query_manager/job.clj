(ns query-manager.job
  "Experimental at this point. Beware!"
  (:refer-clojure :exclude [reset!])
  (:import [java.util.concurrent Executors ThreadFactory])
  (:require [clojure.tools.logging :refer [info error]]
            [clojure.java.jdbc :as jdbc]))

;;-----------------------------------------------------------------------------
;; Thread machinery
;;-----------------------------------------------------------------------------

(let [id-counter (atom 0)]
  (defn- id-gen [] (swap! id-counter inc)))


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

(let [id (atom 0)]
  (defn- job-id [] (swap! id inc)))

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
    (info " - job start:" (:description (:query job)))
    ;;(Thread/sleep (* (inc (rand-int 10)) 1000))
    (try
      (let [sql (:sql (:query job))
            results (doall (jdbc/query db [sql]))
            update (assoc job
                     :status :done
                     :results (take 500 results)
                     :size (count results)
                     :stopped (now))]
        (swap! jobs (fn [js]
                      ;;
                      ;; If the job isn't in the current collection,
                      ;; assume the user deleted it before it completed
                      ;; and drop the results on the cutting room floor.
                      ;;
                      (if (contains? js (:id job))
                        (assoc js (:id job) update)
                        js))))
      (catch Throwable t
        (let [update (assoc job :status :failed :stopped (now) :error (str t))]
          (swap! jobs assoc (:id job) update))
        (error t))
      (finally
        (info " - job complete [" (:description (:query job)) "]")))))

;;-----------------------------------------------------------------------------
;; Public API
;;-----------------------------------------------------------------------------

(defn mk-jobs
  [pool-size]
  {:pool (mk-thread-pool pool-size)
   :pool-size pool-size
   :jobs (atom {})})

(defn create
  [jobs db query]
  (let [new-job (mk-job query)
        runner (mk-runner db (:jobs jobs) new-job)]
    (swap! (:jobs jobs) assoc (:id new-job) new-job)
    (.submit (:pool jobs) runner)
    new-job))

(defn all
  [jobs]
  (if-let [results (vals (deref (:jobs jobs)))]
    (map #(dissoc % :results) results)
    []))

(defn one
  [jobs id]
  (get (deref (:jobs jobs)) id))

(defn delete!
  [jobs id]
  (swap! (:jobs jobs) dissoc (Long/parseLong id)))

(defn reset!
  [jobs]
  (.shutdownNow (:pool jobs))
  (mk-jobs (:db jobs) (:pool-size jobs)))
