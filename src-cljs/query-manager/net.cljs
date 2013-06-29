(ns query-manager.net
  (:require [goog.net.XhrIo :as xhr]))

(defn- jsonify
  [data]
  (JSON/stringify (clj->js data)))

(defn- net-delegate
  [on-success on-failure]
  (fn [response]
    (if (.isSuccess (.-target response))
      (when-not (nil? on-success)
        (on-success (try
                      (.getResponseJson (.-target response))
                      (catch js/Error x
                          (.getResponseText (.-target response))))))
      (when-not (nil? on-failure)
        (on-failure {:status (.getStatus (.-target response))
                     :reason (.getStatusText (.-target response))})))))

(defn- net-request
  [{:keys [uri method on-success on-failure data content-type]}]
  (let [headers (clj->js {:content-type content-type})
        handler (net-delegate on-success on-failure)]
    (.send goog.net.XhrIo uri handler method (jsonify data) headers)))

(defn dump
  [data success failure]
  (net-request
   {:uri "/qman/api/dump"
    :method "POST"
    :on-failure failure
    :on-success success
    :data data
    :content-type "application/json"}))
