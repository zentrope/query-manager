(ns queryizer.data
  (:require [clojure.tools.logging :refer [info]]))

(def default-spec {:type "hsqldb"
                   :database "test"
                   :user "test"
                   :password "test"
                   :host "/tmp/queryizerdb"
                   :port 0})

(def ^:private conn-spec (atom nil))

(defn put-db-spec
  [spec]
  (reset! conn-spec spec))

(defn get-db-spec
  []
  (or @conn-spec default-spec))
