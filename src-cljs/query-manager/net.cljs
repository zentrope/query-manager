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
        :reason (.getStatusText t)})))

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

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn get-db
  [success failure]
  (ajax :uri "/qman/api/db"
        :method "GET"
        :on-failure failure
        :on-success success
        :type :json))

(defn poke-db
  [broadcast]
  (ajax :uri "/qman/api/db"
        :method "GET"
        :on-failure (fn [err]
                      (broadcast [:web-error {:value err}]))
        :on-success (fn [db]
                      (broadcast [:db-change {:value (js->clj db :keywordize-keys true)}]))
        :type :json))

(defn poke-query
  [broadcast]
  (ajax :uri "/qman/api/query"
        :method "GET"
        :on-failure (fn [err]
                      (broadcast [:web-error {:value err}]))
        :on-success (fn [data]
                      (broadcast [:query-change {:value (js->clj data :keywordize-keys true)}]))
        :type :json))

(defn dump
  [data success failure]
  (ajax :uri "/qman/api/dump"
        :method "POST"
        :on-failure failure
        :on-success success
        :data data
        :type :json))
