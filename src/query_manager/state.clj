(ns query-manager.state
  (:require [clojure.core.async :as async]
            [clojure.java.jdbc :as jdbc]))

;;-----------------------------------------------------------------------------
;; Query Stuff
;;-----------------------------------------------------------------------------

;; GLOBAL!!!
(def ^:private query-db (atom {}))

(let [id (atom 0)]
  (defn- id-gen [] (str (swap! id inc))))

(defn all-queries
  []
  (or (vals @query-db) []))

(defn one-query
  [query-id]
  (get @query-db query-id))

(defn create-query!
  [sql description]
  (let [q {:id (id-gen) :sql sql :description description}]
    (swap! query-db assoc (:id q) q)))

(defn delete-query!
  [query-id]
  (swap! query-db dissoc query-id))

(defn update-query!
  [query]
  (swap! query-db assoc (:id query) query))


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
        ch (async/chan)]
    (async/go
     (try
       (jdbc/query spec [sql])
       (async/put! ch {:okay true})
       (catch Throwable t
         (async/put! ch {:okay false :reason (.getMessage t)}))))
    (let [[result _] (async/alts!! [(async/timeout (* 5 1000)) ch])]
      (if (nil? result)
        {:okay false :reason "Network connection attempt timed out."}
        result))))