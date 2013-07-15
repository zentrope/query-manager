(ns query-manager.event
  (:use [query-manager.protocols :only [IBus ISubscriber recv!]]))

;;-----------------------------------------------------------------------------
;; Experimental
;;-----------------------------------------------------------------------------

(defrecord Subscriber [topic handler]
  ISubscriber
  (recv! [this mbus message]
    (handler mbus message)))

(defrecord EventBus [subscriptions]
  IBus
  (subscribe! [this topic handler]
    (let [subscriber (Subscriber. topic handler)]
      (swap! subscriptions (fn [subs]
                             (let [clients (conj (or (topic subs) #{}) subscriber)]
                               (assoc subs topic clients))))))
  (publish! [this topic message]
    (let [subs (topic @subscriptions)]
      (doseq [sub subs]
        (recv! sub this message)))))

(defn mk-event-bus
  []
  (EventBus. (atom {})))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(def ^:private subscriptions (atom {}))

(defn- add-sub!
  [topic f]
  (swap! subscriptions (fn [s]
                       (let [orig (or (topic s) #{})]
                         (assoc s topic (conj orig f))))))

(defn- rem-sub!
  [topic handler]
  (swap! subscriptions (fn [subs]
                       (let [orig (or (topic subs) #{})]
                         (assoc subs topic (disj orig handler))))))

(defn- now
  []
  (.getTime (js/Date.)))

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn subscribe!
  [handler topics]
  (doseq [topic topics]
    (add-sub! topic handler)))

(defn unsubscribe!
  [handler topic]
  (rem-sub! topic handler))

(defn publish!
  [event]
  (let [topic (first event)
        data (assoc (second event) :timestamp (now))]
    (when-let [subscribers (topic @subscriptions)]
      (doseq [subscriber subscribers]
        (subscriber publish! [topic data])))))
