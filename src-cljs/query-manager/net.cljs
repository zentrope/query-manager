(ns query-manager.net
  (:require [goog.net.XhrIo :as xhr]
            [goog.events :as events]
            [query-manager.protocols :refer [publish! subscribe!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- jsonify
  [data]
  (when data
    (JSON/stringify (clj->js data))))

(defn- success
  [response callback {:keys [type]}]
  (let [t (.-target response)]
    (callback (let [text (.getResponseText t)]
                (if (<= (count text) 0)
                  ""
                  (case type
                    :json (.getResponseJson t)
                    text))))))

(defn- failure
  [response callback]
  (let [t (.-target response)]
    (callback {:status (.getStatus t)
               :reason (.getStatusText t)
               :uri (.-lastUri_ t)
               :timestamp (.getTime (js/Date.))})))

(defn- mk-handler
  [{:keys [on-success on-failure] :as opts}]
  (fn [response]
    (if (.isSuccess (.-target response))
      (when on-success
        (success response on-success opts))
      (when on-failure
        (failure response on-failure)))))

(defn- mimetype
  [{:keys [type]}]
  (case type
    :json "application/json"
    "plain/text"))

(defn- ajax
  [& opts]
  (let [{:keys [uri method data type]} opts
        headers (clj->js {goog.net.XhrIo.CONTENT_TYPE_HEADER (mimetype opts)})
        handler (mk-handler opts)
        req (goog.net.XhrIo.)]
    (events/listen req goog.net.EventType/COMPLETE handler)
    (.send req uri method (jsonify data) headers)))

(defn- jread
  [json]
  (js->clj json :keywordize-keys true))

(defn- error-handler
  [mbus]
  (fn [err]
    (publish! mbus :web-error {:value err})))

;;-----------------------------------------------------------------------------
;; Database connection API
;;-----------------------------------------------------------------------------

(defn- poke-db
  [mbus]
  (ajax :uri "/qman/api/db"
        :method "GET"
        :on-failure (error-handler mbus)
        :on-success (fn [db] (publish! mbus :db-change {:value (jread db)}))
        :type :json))

(defn- save-db
  [mbus db]
  (ajax :uri "/qman/api/db"
        :method "PUT"
        :on-failure (fn [err] (publish! mbus :web-error {:value err}))
        :on-success (fn [_] (poke-db mbus))
        :data db
        :type :json))

;;-----------------------------------------------------------------------------
;; Jobs API
;;-----------------------------------------------------------------------------

(defn- poke-jobs
  [mbus]
  (ajax :uri "/qman/api/job"
        :method "GET"
        :type :json
        :on-failure (error-handler mbus)
        :on-success (fn [jobs] (publish! mbus :job-change {:value (jread jobs)}))))

(defn- poke-job
  [mbus job-id]
  (ajax :uri (str "/qman/api/job/" job-id)
        :method "GET"
        :type :json
        :on-failure (error-handler mbus)
        :on-success (fn [job] (publish! mbus :job-get {:value (jread job)}))))

(defn- run-job
  [mbus query-id]
  (ajax :uri (str "/qman/api/job/" query-id)
        :method "POST"
        :on-failure (error-handler mbus)
        :on-success (fn [_] (poke-jobs mbus))))

(defn- delete-job
  [mbus job-id]
  (ajax :uri (str "/qman/api/job/" job-id)
        :method "DELETE"
        :type :json
        :on-failure (error-handler mbus)
        :on-success (fn [_] (poke-jobs mbus))))

;;-----------------------------------------------------------------------------
;; Queries API
;;-----------------------------------------------------------------------------

(defn- poke-queries
  [mbus]
  (ajax :uri "/qman/api/query"
        :method "GET"
        :on-failure (error-handler mbus)
        :on-success (fn [data] (publish! mbus :query-change {:value (jread data)}))
        :type :json))

(defn- poke-query
  [mbus id]
  (ajax :uri (str "/qman/api/query/" id)
        :method "GET"
        :on-failure (error-handler mbus)
        :on-success (fn [data] (publish! mbus :query-get {:value (jread data)}))
        :type :json))

(defn- save-query
  [mbus query]
  (ajax :uri "/qman/api/query"
        :method "POST"
        :data query
        :type :json
        :on-failure (error-handler mbus)
        :on-success (fn [_] (poke-queries mbus))))

(defn- update-query
  [mbus query]
  (ajax :uri (str "/qman/api/query/" (:id query))
        :method "PUT"
        :data query
        :type :json
        :on-failure (error-handler mbus)
        :on-success (fn [_] (poke-queries mbus))))

(defn- delete-query
  [mbus query-id]
  (ajax :uri (str "/qman/api/query/" query-id)
        :method "DELETE"
        :type :json
        :on-failure (error-handler mbus)
        :on-success (fn [_] (poke-queries mbus))))

(def ^:private subscriptions
  {:db-save (fn [mbus msg] (save-db mbus (:value msg)))
   :db-poke (fn [mbus _] (poke-db mbus))
   :queries-poke (fn [mbus _] (poke-queries mbus))
   :query-save (fn [mbus msg] (save-query mbus (:value msg)))
   :query-update (fn [mbus msg] (update-query mbus (:value msg)))
   :query-delete (fn [mbus msg] (delete-query mbus (:value msg)))
   :query-run (fn [mbus msg] (run-job mbus (:value msg)))
   :query-poke (fn [mbus msg] (poke-query mbus (:value msg)))
   :jobs-poke (fn [mbus _] (poke-jobs mbus))
   :job-poke (fn [mbus msg] (poke-job mbus (:value msg)))
   :job-delete (fn [mbus msg] (delete-job mbus (:value msg)))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn init!
  [mbus]
  (doseq [[topic handler] subscriptions]
    (subscribe! mbus topic handler)))
