(ns queryizer.test-api
  (:require [clojure.test :refer [deftest is]]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :refer [write-str read-str]]
            [queryizer.http :refer [app]]
            [queryizer.data :refer [default-spec]]))

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

;;-----------------------------------------------------------------------------
;; Tests
;;-----------------------------------------------------------------------------

(deftest test-test
  (is (= 1 1)))

(deftest test-get-db
  (put! "/qzer/api/db" default-spec)
  (let [r (get! "/qzer/api/db")
        data (jread (:body r))]
    (is (= 200 (:status r)))
    (is (= data default-spec))))

(deftest test-put-db
  (let [spec {:user "k" :password "z" :type "postgresl" :host "foo" :port 17}
        r (put! "/qzer/api/db" spec)
        r2 (get! "/qzer/api/db")
        data (jread (:body r2))]
    (is (= 201 (:status r)))
    (is (= spec data))))
