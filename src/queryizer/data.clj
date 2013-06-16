(ns queryizer.data
  (:require [clojure.tools.logging :refer [info]]))

(def default-spec {:classname "org.h2.Driver"
                   :subprotocol "h2:mem"
                   :subname "test"
                   :user "sa"
                   :password ""
                   :DB_CLOSE_DELAY -1})

(def ^:private conn-spec (atom nil))

(defn put-db-spec
  [spec]
  (reset! conn-spec spec))

(defn get-db-spec
  []
  (or @conn-spec default-spec))
