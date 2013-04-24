

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
	(table :unix)
	(database db))

(def query-finished (promise))

(defn run-query [query]
	(println "QUERY ID: " query)
	(exec-raw [query] :results))

(def available-queries
	(read-string 
		(slurp (io/resource "queries"))))

(defn query [id] ))
	(:sql (first (filter #(= id (:id % )) available-queries))))

