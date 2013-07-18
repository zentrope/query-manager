(ns query-manager.test-db
  (:require [clojure.java.jdbc :as jdbc]))

(defn- spawn!
  [f]
  (doto (Thread. f)
    (.setName "test-db-conn")
    (.setDaemon true)
    (.start)))

(defn- mk-conn-test-query
  [db]
  (case (:type (keyword db))
    :oracle "select 1 from dual"
    "select 1"))

(defn- run-conn-test
  [spec lock]
  (try
    (jdbc/query spec [(mk-conn-test-query spec)])
    (deliver lock {:okay true})
    (catch Throwable t
      (deliver lock {:okay false :reason (.getMessage t)}))))

(defn test-connection
  [spec]
  (let [wait-lock (promise)
        timeout (* 15 1000)]
    (spawn! (fn [] (run-conn-test spec wait-lock)))
    (deref wait-lock timeout {:okay false
                              :reason "Network connection attempt timed out."})))
