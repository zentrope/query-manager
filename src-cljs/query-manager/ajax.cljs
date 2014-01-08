(ns query-manager.ajax
  (:require
    [goog.net.XhrIo :as xhr]
    [goog.events :as events]))

(defn- uuid
  "returns a type 4 random UUID: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
   http://catamorphic.wordpress.com/2012/03/02/generating-a-random-uuid-in-clojurescript/"
  []
  (let [r (repeatedly 30 (fn [] (.toString (rand-int 16) 16)))]
    (apply str (concat (take 8 r) ["-"]
                       (take 4 (drop 8 r)) ["-4"]
                       (take 3 (drop 12 r)) ["-"]
                       [(.toString  (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16)]
                       (take 3 (drop 15 r)) ["-"]
                       (take 12 (drop 18 r))))))

(def ^:private client-id
  "Generated once per loaded script."
  (uuid))

(defn- jsonify
  [data]
  (when data
    (JSON/stringify (clj->js data))))

(defn- success
  [response callback {:keys [type]}]
  (let [t (.-target response)]
    (callback
      (let [text (.getResponseText t)]
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
        headers (clj->js {goog.net.XhrIo.CONTENT_TYPE_HEADER (mimetype opts)
                          "client-id" client-id})
        handler (mk-handler opts)
        req (goog.net.XhrIo.)]
    (events/listen req goog.net.EventType/COMPLETE handler)
    (.send req uri method (jsonify data) headers)))
