

(ns queryizer.controller
	(:use korma.db)
	(:use korma.core))

(defdb db (mysql {:db "te"
	              :user "root"
	              :password "services"
	              :host "localhost"
	              :port "3306"}))

(defentity unix
	(table :unix)
	(database db))

(defn run-query [query]
	(str (exec-raw [query] :results)))

(def available-queries
	(read-string 
		(slurp "resources/queries")))

