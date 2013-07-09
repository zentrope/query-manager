(ns query-manager.net
  (:require [goog.net.XhrIo :as xhr]
            [goog.events :as events]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- jsonify
  [data]
  (when data
    (JSON/stringify (clj->js data))))

(defn- success
  [response f {:keys [type]}]
  (let [t (.-target response)]
    (f (let [text (.getResponseText t)]
         (if (<= (count text) 0)
           ""
           (case type
             :json (.getResponseJson t)
             text))))))

(defn- failure
  [response f]
  (let [t (.-target response)]
    (f {:status (.getStatus t)
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
        headers (clj->js {:content-type (mimetype opts)})
        handler (mk-handler opts)]
    (.send goog.net.XhrIo uri handler method (jsonify data) headers)))

(defn- jread
  [json]
  (js->clj json :keywordize-keys true))

(defn- error-handler
  [broadcast]
  (fn [err]
    (broadcast [:web-error {:value err}])))

;;-----------------------------------------------------------------------------
;; Database connection API
;;-----------------------------------------------------------------------------

(defn- poke-db
  [broadcast]
  (ajax :uri "/qman/api/db"
        :method "GET"
        :on-failure (error-handler broadcast)
        :on-success (fn [db] (broadcast [:db-change {:value (jread db)}]))
        :type :json))

(defn- save-db
  [broadcast db]
  (ajax :uri "/qman/api/db"
        :method "PUT"
        :on-failure (fn [err] (broadcast [:web-error {:value err}]))
        :on-success (fn [_] (poke-db broadcast))
        :data db
        :type :json))

;;-----------------------------------------------------------------------------
;; Jobs API
;;-----------------------------------------------------------------------------

(defn- poke-jobs
  [broadcast]
  (ajax :uri "/qman/api/job"
        :method "GET"
        :type :json
        :on-failure (error-handler broadcast)
        :on-success (fn [jobs] (broadcast [:job-change {:value (jread jobs)}]))))

(defn- poke-job
  [broadcast job-id]
  (ajax :uri (str "/qman/api/job/" job-id)
        :method "GET"
        :type :json
        :on-failure (error-handler broadcast)
        :on-success (fn [job] (broadcast [:job-get {:value (jread job)}]))))

(defn- run-job
  [broadcast query-id]
  (ajax :uri (str "/qman/api/job/" query-id)
        :method "POST"
        :on-failure (error-handler broadcast)
        :on-success (fn [_] (poke-jobs broadcast))))

(defn- delete-job
  [broadcast job-id]
  (ajax :uri (str "/qman/api/job/" job-id)
        :method "DELETE"
        :type :json
        :on-failure (error-handler broadcast)
        :on-success (fn [_] (poke-jobs broadcast))))

;;-----------------------------------------------------------------------------
;; Queries API
;;-----------------------------------------------------------------------------

(defn- poke-queries
  [broadcast]
  (ajax :uri "/qman/api/query"
        :method "GET"
        :on-failure (error-handler broadcast)
        :on-success (fn [data] (broadcast [:query-change {:value (jread data)}]))
        :type :json))

(defn- poke-query
  [broadcast id]
  (ajax :uri (str "/qman/api/query/" id)
        :method "GET"
        :on-failure (error-handler broadcast)
        :on-success (fn [data] (broadcast [:query-get {:value (jread data)}]))
        :type :json))

(defn- save-query
  [broadcast query]
  (ajax :uri "/qman/api/query"
        :method "POST"
        :data query
        :type :json
        :on-failure (error-handler broadcast)
        :on-success (fn [_] (poke-queries broadcast))))

(defn- update-query
  [broadcast query]
  (ajax :uri (str "/qman/api/query/" (:id query))
        :method "PUT"
        :data query
        :type :json
        :on-failure (error-handler broadcast)
        :on-success (fn [_] (poke-queries broadcast))))

(defn- delete-query
  [broadcast query-id]
  (ajax :uri (str "/qman/api/query/" query-id)
        :method "DELETE"
        :type :json
        :on-failure (error-handler broadcast)
        :on-success (fn [_] (poke-queries broadcast))))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn events
  "Events this namespace is interested in receiving."
  []
  [:db-poke :db-save
   :query-poke :query-save :query-delete :query-run
   :queries-poke :query-update
   :jobs-poke :job-delete :job-poke])

(defn recv
  "Event receiver."
  [broadcast [topic event]]
  (case topic
    ;;
    ;; database events
    ;;
    :db-save (save-db broadcast (:value event))
    :db-poke (poke-db broadcast)
    ;;
    ;; query events
    ;;
    :queries-poke (poke-queries broadcast)
    :query-save (save-query broadcast (:value event))
    :query-update (update-query broadcast (:value event))
    :query-delete (delete-query broadcast (:value event))
    :query-run (run-job broadcast (:value event))
    :query-poke (poke-query broadcast (:value event))
    ;;
    ;; job events
    ;;
    :jobs-poke (poke-jobs broadcast)
    :job-poke (poke-job broadcast (:value event))
    :job-delete (delete-job broadcast (:value event))
    ;;
    ;;
    true))
