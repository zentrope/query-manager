(ns query-manager.db
  (:refer-clojure :exclude [get put load]))

(def default-spec {:type "h2"
                   :user "sa"
                   :password ""
                   :database "test"
                   :host "127.0.0.1"
                   :port 1234
                   :updated false})

(def ^:private conn-spec (atom default-spec))

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
  [instance new-spec]
  (swap! instance assoc :spec (assoc new-spec :updated true)))

(defn get
  [instance]
  (:spec @instance))

(defn spec
  [instance]
  (specialize (get instance)))

(defn instance
  []
  (atom {:spec default-spec}))
