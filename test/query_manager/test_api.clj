(ns query-manager.test-api
  (:import [java.util UUID])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is use-fixtures testing]]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :refer [write-str read-str]]
            [query-manager.http :refer [app]]
            [query-manager.data :refer [default-spec]]))

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
  (doseq [i (range 1 100)]
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

(defn clean-test-data
  [test-function]
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
    (is (= data default-spec))))

(deftest put-db
  (let [spec {:user "k" :password "z" :subname "test" :subprotocol "postgresql" :host "foo" :port 17}
        r (put! "/qman/api/db" spec)
        r2 (get! "/qman/api/db")
        data (jread (:body r2))]
    (is (= 201 (:status r)))
    (is (= spec data))))

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
