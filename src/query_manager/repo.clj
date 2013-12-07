(ns query-manager.repo
  (:import (java.io File))
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

;;-----------------------------------------------------------------------------

(def ^:private sep
  File/separator)

(def ^:private user-dir
  (System/getProperty "user.dir"))

(defn- path->
  [& strings]
  (string/join sep strings))

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

(defn- write-to!
  [^File f doc]
  (try
    (with-open [out (io/writer f)]
      (ensure-dir! (.getParentFile f))
      (pprint doc out)
      (log/info "wrote a document to" (path-of f)))
    (catch Throwable t
      (log/error "unable to write a document to" (path-of f)))))

(defn- read-from!
  [^File f]
  (try
    (when (.exists f)
      (edn/read-string (slurp f)))
    (catch Throwable t
      (log/error "unable to read from" (path-of f))
      nil)))

;;-----------------------------------------------------------------------------

(defn save-database!
  [repo database]
  (let [place (file-> (:root-dir repo) "database.clj")]
    (write-to! place database)))

(defn load-database!
  [repo]
  (read-from! (file-> (:root-dir repo) "database.clj")))

(defn start!
  [repo]
  (log/info "Starting file repo service.")
  (doseq [[_ path] repo]
    (ensure-dir! (io/as-file path)))
  :started)

(defn stop!
  [repo]
  (log/info "Stopping file repo service.")
  :stopped)

(defn instance
  []
  (let [root (path-> user-dir "qm-work")]
    {:root-dir root
     :query-dir (path-> root "queries")
     :job-dir (path-> root "jobs")}))
