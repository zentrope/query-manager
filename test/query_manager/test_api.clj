(ns query-manager.test-api
  (:import [java.util UUID]
           [org.slf4j LoggerFactory]
           [ch.qos.logback.classic Level Logger])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is use-fixtures testing]]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :refer [write-str read-str]]
            [query-manager.http :refer [mk-web-app]]
            [query-manager.job :refer [mk-jobs]]
            [query-manager.db :refer [default-spec specialize]]))

;;-----------------------------------------------------------------------------
;; Database Functions
;;-----------------------------------------------------------------------------

(def ^:private db {:classname "org.h2.Driver"
                   :subprotocol "h2:mem"
                   :subname "test"
                   :user "sa"
                   :password ""
                   :DB_CLOSE_DELAY -1})

(def ^:private statement {:drop "drop table test if exists"
                          :create (str "create table if not exists test "
                                       "(id long identity, name varchar(100))")
                          :insert "insert into test (name) values (?)"
                          :select "select * from test order by id"})

(defn- create-db
  [db]
  (jdbc/db-do-commands db true (:create statement))
  (doseq [i (range 0 100)]
    (jdbc/execute! db [(:insert statement) (str (UUID/randomUUID))])))

(defn- data-db
  [db]
  (jdbc/query db [(:select statement)]))

(defn- drop-db
  [db]
  (jdbc/db-do-commands db true (:drop statement)))

;;-----------------------------------------------------------------------------
;; Convenience Functions
;;-----------------------------------------------------------------------------

(defn- pause
  [seconds]
  (Thread/sleep (* seconds 1000)))

(def ^:private app (mk-web-app (mk-jobs 100)))

(defn- jread
  [string]
  (read-str string :key-fn keyword))

(defn- get!
  [resource & params]
  (app {:request-method :get :uri resource :params (first params)}))

(defn- put!
  [resource body]
  (app {:request-method :put :uri resource :body (write-str body)}))

(defn- post!
  [resource body]
  (app {:request-method :post :uri resource :body (write-str body)}))

(defn- delete!
  [resource]
  (app {:request-method :delete :uri resource}))

;;-----------------------------------------------------------------------------
;; Fixtures
;;-----------------------------------------------------------------------------

(defn- mute-logging
  []
  (doto (LoggerFactory/getLogger Logger/ROOT_LOGGER_NAME)
    (.setLevel Level/ERROR)))

(defn clean-test-data
  [test-function]
  ;;
  (mute-logging)
  ;;
  ;; Restore the default database spec
  ;;
  (put! "/qman/api/db" default-spec)
  ;;
  ;; Remove any stored queries
  ;;
  (doseq [q (jread (:body (get! "/qman/api/query")))]
    (delete! (str "/qman/api/query/" (:id q))))
  ;;
  ;; Remove any stored jobs
  ;;
  (doseq [j (jread (:body (get! "/qman/api/job")))]
    (delete! (str "/qman/api/job/" (:id j))))
  ;;
  ;; Run the test
  ;;
  (test-function))

(use-fixtures :each clean-test-data)

;;-----------------------------------------------------------------------------
;; Tests
;;-----------------------------------------------------------------------------

(deftest get-db
  (let [r (get! "/qman/api/db")
        data (jread (:body r))]
    (is (= 200 (:status r)))
    (is (= (specialize default-spec) data))))

(deftest put-db
  (let [spec {:user "k" :password "z" :database "test" :type "mysql"
              :host "foo" :port 17}
        r (put! "/qman/api/db" spec)
        r2 (get! "/qman/api/db")
        data (jread (:body r2))]
    (is (= 201 (:status r)))
    (is (= (specialize spec) data))))

(deftest create-and-list-sql
  (let [query {:sql "show tables" :description "List tables."}
        r1 (post! "/qman/api/query" query)
        r2 (get! "/qman/api/query")
        q (first (jread (:body r2)))]

    (testing "posting a query"
      (is (= 201 (:status r1)))
      (is (not (nil? (:id q))))
      (is (= query (dissoc q :id))))

    (testing "getting list of queries"
      (is (= 200 (:status r2))))

    (testing "getting a specific query"
      (is (= q (jread (:body (get! (str "/qman/api/query/" (:id q))))))))))

(deftest update-sql
  (let [q1 {:sql "show tables" :description "List tables."}
        r1 (post! "/qman/api/query" q1)
        q2 (first (jread (:body (get! "/qman/api/query"))))
        q3 (merge q2 {:sql "show other" :description "Test"})
        r2 (put! (str "/qman/api/query/" (:id q2)) q3)
        q4 (jread (:body (get! (str "/qman/api/query/" (:id q2)))))
        r3 (delete! (str "/qman/api/query/" (:id q2)))
        r4 (get! (str "/qman/api/query/" (:id q2)))]

    (testing "Create the query."
      (is (= 201 (:status r1))))

    (testing "Verify updated query comes back updated."
      (is (= 201 (:status r2)))
      (is (= q3 q4)))

    (testing "Verify deleted query is gone."
      (is (= 201 (:status r3)))
      (is (= 404 (:status r4))))))

(deftest create-and-list-job
  (drop-db db)
  (create-db db)
  (let [r1 (post! "/qman/api/query" {:sql (:select statement) :description "Test"})
        q1 (first (jread (:body (get! "/qman/api/query"))))
        r2 (post! (str "/qman/api/job/" (:id q1)) [])
        _ (pause 1)
        r3 (get! "/qman/api/job")
        jobs (jread (:body r3))
        job (first jobs)]
    (is (= 201 (:status r1)))
    (is (= 201 (:status r2)))
    (is (= 200 (:status r3)))
    (is (= 1 (count jobs)))
    (is (= "done" (:status job)))
    (is (= (:select statement) (get-in job [:query :sql])))
    (is (= "Test" (get-in job [:query :description])))
    (is (= job (select-keys job [:id :started :stopped :query :status])))))

(deftest find-one-job-among-many
  (drop-db db)
  (create-db db)
  (let [r1 (post! "/qman/api/query" {:sql (:select statement) :description "Test"})
        q1 (first (jread (:body (get! "/qman/api/query"))))
        _ (dotimes [n 5] (post! (str "/qman/api/job/" (:id q1)) []))
        _ (pause 1)
        jobs (jread (:body (get! "/qman/api/job")))
        job (jread (:body (get! "/qman/api/job/3")))]
    (is (= 5 (count jobs)))
    (is (> (:stopped job) (:started job)))
    (is (= 100 (count (:results job))))
    (is (= 3 (:id job)))))
