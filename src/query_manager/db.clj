(ns query-manager.db
  (:refer-clojure :exclude [get put load])
  (:import [java.io File])
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

;; TODO:
;;  - move have this return a state object rather than use globals.
;;  - move file read/write to general lib so we can write queries, jobs, etc.
;;  - cryptosize password

(def ^:private sep
  File/separator)

(def ^:private user-dir
  (System/getProperty "user.dir"))

(def ^:private db-file
  (io/as-file (str user-dir sep "qm-work" sep "database.clj")))

(def default-spec {:type "h2"
                   :user "sa"
                   :password ""
                   :database "test"
                   :host "127.0.0.1"
                   :port 1234
                   :updated false})

(def ^:private conn-spec (atom default-spec))

(defn- path->
  [^File f]
  (.getAbsolutePath f))

(defmulti specialize (fn [spec] (keyword (:type spec))))

(defmethod specialize :default
  [spec]
  {:type (:type spec)
   :host (:host spec)
   :port (:port spec)
   :user (:user spec)
   :password (:password spec)
   :subname (str "//" (:host spec) ":" (:port spec) "/" (:database spec))
   :subprotocol (name (:type spec))})

(defmethod specialize :sqlserver
  [spec]
  {:type (:type spec)
   :host (:host spec)
   :port (:port spec)
   :user (:user spec)
   :password (:password spec)
   :subname (str "//" (:host spec) ":" (:port spec) "/" (:database spec))
   :subprotocol "jtds:sqlserver"})

(defmethod specialize :oracle
  [spec]
  {:type (:type spec)
   :host (:host spec)
   :port (:port spec)
   :user (:user spec)
   :password (:password spec)
   :subname (str "@" (:host spec) ":" (:port spec) ":" (:database spec))
   :subprotocol "oracle:thin"
   :classname "oracle.jdbc.driver.OracleDriver"})

(defmethod specialize :h2
  [spec]
  {:type "h2"
   :classname "org.h2.Driver"
   :subprotocol "h2:mem"
   :subname (:database spec)
   :user (:user spec)
   :password (:password spec)
   :DB_CLOSE_DELAY -1})

(defn put
  [spec]
  (reset! conn-spec (assoc spec :updated true)))

(defn- ensure-parent!
  [^File f]
  (let [dir (if (.isDirectory f) f (.getParentFile f))]
    (when-not (.exists dir)
      (log/info "Creating work directory: " (path-> dir))
      (.mkdirs dir))))

(defn save
  []
  (try
    (ensure-parent! db-file)
    (with-open [w (io/writer db-file)]
      (pprint [@conn-spec] w)
      (log/info "Wrote currently defined database spec file:" (path-> db-file)))
    (catch Throwable t
      (log/error "Unable to write database spec file: " t))))

(defn load
  []
  (try
    (when (.exists db-file)
      (let [spec (first (edn/read-string (slurp db-file)))]
        (put spec)
        (log/info "Loaded previously cached database spec file:" (path-> db-file))))
    (catch Throwable t
      (log/error "Unable to load database spec file: " t))))

(defn spec
  []
  (specialize @conn-spec))

(defn get
  []
  @conn-spec)
