

(ns queryizer.controller
	(:use korma.db)
	(:use korma.core))

(defdb db (mysql {:db "te"
	              :user "root"
	              :password "services"}))

(defentity unix
	(table :unix)
	(database db))

(defn myquery []
	(select* unix))