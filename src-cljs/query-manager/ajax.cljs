(ns query-manager.ajax
  (:require [goog.net.XhrIo :as xhr]
            [goog.events :as events]))

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

(defn ajax
  [& opts]
  (let [{:keys [uri method data type]} opts
        headers (clj->js {goog.net.XhrIo.CONTENT_TYPE_HEADER (mimetype opts)})
        handler (mk-handler opts)
        req (goog.net.XhrIo.)]
    (events/listen req goog.net.EventType/COMPLETE handler)
    (.send req uri method (jsonify data) headers)))
