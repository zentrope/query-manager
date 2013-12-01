(ns query-manager.sql)

(def ^:private query-db (atom {}))

(let [id (atom 0)]
  (defn- id-gen [] (str (swap! id inc))))

(defn all
  []
  (or (vals @query-db) []))

(defn one
  [query-id]
  (get @query-db query-id))

(defn create!
  [sql description]
  (let [q {:id (id-gen) :sql sql :description description}]
    (swap! query-db assoc (:id q) q)))

(defn delete!
  [query-id]
  (swap! query-db dissoc query-id))

(defn update!
  [query]
  (swap! query-db assoc (:id query) query))
