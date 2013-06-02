(ns queryizer.controller
  (:use korma.db)
  (:use korma.core)
  (:require [clojure.java.io :as io]))

(defdb db (mysql {:db "te"
  :user "root"
  :password "services"
  :host "localhost"
  :port "3306"}))

(defentity unix
  (table :unix )
  (database db))

(def query-finished (promise))
;;mmake UUID for query result
(def make-id
  "Makes a UUID for uniquely"
  (str (java.util.UUID/randomUUID)))

;;runs given query using korma
(defn run-query [query]
  (let [results (exec-raw [query] :results) 
    id make-id]
    (println "QUERY ID: " query)
    (spit (io/resource "jobs") 
      (apply str ["{:id " id " :results " results "}"])) results))

;;reads in "resources/queries"
(def available-queries
  (read-string
    (slurp (io/resource "queries"))))

;;returns sql quesry associated with id
(defn query [id]
  (:sql (first (filter #(= id (:id %)) available-queries))))

(def list-jobs
  (delay
    (read-string
      (slurp (io/resource "jobs")))))

