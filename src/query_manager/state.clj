(ns query-manager.state
  ;;
  ;; This module stores everything regarding state, and uses plenty of
  ;; global variables! I know, I know. Not kosher. But once
  ;; everything's in place and the rest of the APP is how I want it
  ;; (for now), I can take a look at all the concerns here and
  ;; refactor appropriately. How's that for defensiveness?
  ;;
  (:import (java.io File))
  (:require [clojure.core.async :refer [put! timeout alts!! go thread-call chan]]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.string :refer [lower-case trim join]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]))

;;-----------------------------------------------------------------------------

(defn- id-gen
  []
  (clojure.string/replace (str (java.util.UUID/randomUUID)) #"-" ""))

;;-----------------------------------------------------------------------------
;; Repo
;;-----------------------------------------------------------------------------

(def ^:private sep
  File/separator)

(def ^:private user-dir
  (System/getProperty "user.dir"))

(defn- path->
  [& strings]
  (join sep strings))

(defn- file->
  [& strings]
  (io/as-file (apply path-> strings)))

(defn- path-of
  [^File f]
  (.getAbsolutePath f))

(defn- ensure-dir!
  [^File dir]
  (when-not (.exists dir)
    (log/info  "creating cache directory: " (path-> dir))
    (.mkdirs dir)))

(defn- delete!
  [^File file]
  (when (.exists file)
    (.delete file)))

(defn- write-to!
  [^File f doc]
  (try
    (ensure-dir! (.getParentFile f))
    (with-open [out (io/writer f)]
      (pprint doc out)
      (log/info "wrote a document to" (path-of f)))
    (catch Throwable t
      (log/error "unable to write a document to" (path-of f) " : " (.getMessage t)))))

(defn- read-from!
  [^File f]
  (try
    (when (.exists f)
      (edn/read-string (slurp f)))
    (catch Throwable t
      (log/error "unable to read from" (path-of f))
      nil)))

(defn- file-name
  [id]
  (if (> (count id) 10)
    (str id ".clj")
    (let [zs "0000000000"
          sid (str id)]
      (str (subs zs 0 (- 10 (count sid))) sid ".clj"))))

;; GLOBAL
(def ^:private root-dir
  (atom (path-> user-dir "qm-work")))

(defn- file-only-seq
  [^File place]
  (filter (fn [f] (.isFile f)) (file-seq place)))

(defn- batch-load!
  [subdir load-fn]
  (let [place (file-> @root-dir subdir)
        files (file-only-seq place)]
    (doseq [f files]
      (let [q (read-from! f)]
        (load-fn q)))))

;;-----------------------------------------------------------------------------

(defn save-database!
  [database]
  (let [place (file-> @root-dir "database.clj")]
    (write-to! place database)))

(declare put-db!)

(defn load-database!
  []
  (when-let [db (read-from! (file-> @root-dir "database.clj"))]
    (put-db! db)))

;;-----

(defn- save-job-to-disk!
  [job]
  (let [place (file-> @root-dir "jobs" (file-name (:id job)))]
    (write-to! place job)))

(defn- remove-job-from-disk!
  [job]
  (delete! (file-> @root-dir "jobs" (file-name (:id job)))))

(declare load-job!)

(defn load-jobs-from-disk!
  []
  (batch-load! "jobs" load-job!))

;;-----

(declare load-query!)

(defn load-queries-from-disk!
  []
  (batch-load! "queries" load-query!))

(defn- save-query-to-disk!
  [query]
  (let [place (file-> @root-dir "queries" (file-name (:id query)))]
    (write-to! place query)))

(defn- remove-query-from-disk!
  [query]
  (delete! (file-> @root-dir "queries" (file-name (:id query)))))

;;-----------------------------------------------------------------------------
;; Query Stuff
;;-----------------------------------------------------------------------------

;; GLOBAL!!!
(def ^:private query-db (atom {}))

(defn all-queries
  []
  (or (vals @query-db) []))

(defn one-query
  [query-id]
  (get @query-db query-id))

(defn- load-query!
  [query]
  (swap! query-db assoc (:id query) query))

(defn create-query!
  [sql description]
  (let [q {:id (id-gen) :sql sql :description description}]
    (save-query-to-disk! q)
    (swap! query-db assoc (:id q) q)))

(defn delete-query!
  [query-id]
  (remove-query-from-disk! {:id query-id})
  (swap! query-db dissoc query-id))

(defn update-query!
  [query]
  (save-query-to-disk! query)
  (swap! query-db assoc (:id query) query))

;;-----------------------------------------------------------------------------
;; Job Stuff
;;-----------------------------------------------------------------------------

(defn- now
  []
  (System/currentTimeMillis))

(defn- mk-job
  [query]
  {:id (id-gen)
   :started (now)
   :stopped -1
   :query query
   :status :pending
   :results []})

(defn- calc-count-or-size
  "Given a result set, return its size unless there's a single row
   with a single column named 'count', in which case, return that."
  [results]
  (let [size (count results)
        row (first results)
        cols (count (keys (or row {})))]
    (if (and (= cols 1) (:count row) (< size 2))
      (:count row)
      nil)))

(defn- throw-if-not-runnable
  [sql]
  (let [q (lower-case (trim sql))
        runnable? (or (.startsWith q "select")
                      (.startsWith q "show"))]
    (when-not runnable?
      (throw (Exception. "Query must start with 'select'. Sorry!")))))

(defn- mk-runner
  [db jobs job response-q]
  (fn []
    (log/info " --: job start: [" (:description (:query job)) "]")
    (try
      (throw-if-not-runnable (:sql (:query job)))
      (let [sql (:sql (:query job))
            results (doall (take 500 (jdbc/query db [sql] :identifiers identity)))
            update (assoc job
                     :status :done
                     :results results
                     :size (count results)
                     :count-col (calc-count-or-size results)
                     :stopped (now))]
        (save-job-to-disk! update)
        (swap! jobs (fn [js]
                      ;;
                      ;; If the job isn't in the current collection,
                      ;; assume the user deleted it before it
                      ;; completed and drop the results on the cutting
                      ;; room floor.
                      ;;
                      (if (contains? js (:id job))
                        (assoc js (:id job) update)
                        js))))
      (catch Throwable t
        (let [update (assoc job :status :failed :stopped (now) :error (str t))]
          (save-job-to-disk! update)
          (swap! jobs assoc (:id job) update))
        (log/error t))
      (finally
        (put! response-q [:job-complete job])
        (log/info " --: job complete: [" (:description (:query job)) "]")))))

;; GLOBAL!!!
(def ^:private +jobs+
  (atom {}))

(defn- load-job!
  [job]
  (swap! +jobs+ assoc (:id job) job))

(defn create-job!
  [db query response-q]
  (let [new-job (mk-job query)
        runner (mk-runner db +jobs+ new-job response-q)]
    (save-job-to-disk! new-job)
    (swap! +jobs+ assoc (:id new-job) new-job)
    (thread-call runner)
    new-job))

(defn all-jobs
  []
  (if-let [results (vals (deref +jobs+))]
    (map #(dissoc % :results) results)
    []))

(defn one-job
  [id]
  (get (deref +jobs+) id))

(defn delete-job!
  [id]
  (remove-job-from-disk! {:id id})
  (swap! +jobs+ dissoc id))

(defn delete-all-jobs!
  []
  (doseq [[id job] @+jobs+]
    (remove-job-from-disk! {:id id}))
  (reset! +jobs+ {}))

;;-----------------------------------------------------------------------------
;; DB Stuff
;;-----------------------------------------------------------------------------

(def ^:private default-spec
  {:type "h2"
   :user "sa"
   :password ""
   :database "test"
   :host "127.0.0.1"
   :port 1234
   :updated false})

;; GLOBAL!!!
(def ^:private conn-spec
  (atom default-spec))

(defmulti db-specialize
  (fn [spec] (keyword (:type spec))))

(defmethod db-specialize :default
  [spec]
  {:type (:type spec)
   :host (:host spec)
   :port (:port spec)
   :user (:user spec)
   :password (:password spec)
   :subname (str "//" (:host spec) ":" (:port spec) "/" (:database spec))
   :subprotocol (name (:type spec))})

(defmethod db-specialize :sqlserver
  [spec]
  {:type (:type spec)
   :host (:host spec)
   :port (:port spec)
   :user (:user spec)
   :password (:password spec)
   :subname (str "//" (:host spec) ":" (:port spec) "/" (:database spec))
   :subprotocol "jtds:sqlserver"})

(defmethod db-specialize :oracle
  [spec]
  {:type (:type spec)
   :host (:host spec)
   :port (:port spec)
   :user (:user spec)
   :password (:password spec)
   :subname (str "@" (:host spec) ":" (:port spec) ":" (:database spec))
   :subprotocol "oracle:thin"
   :classname "oracle.jdbc.driver.OracleDriver"})

(defmethod db-specialize :h2
  [spec]
  {:type "h2"
   :classname "org.h2.Driver"
   :subprotocol "h2:mem"
   :subname (:database spec)
   :user (:user spec)
   :password (:password spec)
   :DB_CLOSE_DELAY -1})

(defn put-db!
  [new-spec]
  (reset! conn-spec (assoc new-spec :updated true)))

(defn get-db
  []
  @conn-spec)

(defn db-spec
  []
  (db-specialize @conn-spec))

(defn test-conn!
  [spec]
  (let [sql (case (keyword (:type spec)) :oracle "select 1 from dual" "select 1")
        ch (chan)]
    (go
     (try
       (jdbc/query spec [sql])
       (put! ch {:okay true})
       (catch Throwable t
         (put! ch {:okay false :reason (.getMessage t)}))))
    (let [[result _] (alts!! [(timeout (* 5 1000)) ch])]
      (if (nil? result)
        {:okay false :reason "Network connection attempt timed out."}
        result))))
