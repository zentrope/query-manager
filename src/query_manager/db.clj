(ns query-manager.db
  (:refer-clojure :exclude [get put]))

(def default-spec {:classname "org.h2.Driver"
                   :subprotocol "h2:mem"
                   :subname "test"
                   :user "sa"
                   :password ""
                   :DB_CLOSE_DELAY -1})

(def ^:private conn-spec (atom default-spec))

(defn put
  [spec]
  (reset! conn-spec spec))

(defn get
  []
  conn-spec)
