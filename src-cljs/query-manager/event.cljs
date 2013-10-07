(ns query-manager.event
  (:require [query-manager.protocols :as proto]))

(defrecord Subscriber [topic handler]
  proto/ISubscriber
  (recv! [this mbus message]
    (handler mbus message)))

(defrecord EventBus [subscriptions]
  proto/IBus
  (unsubscribe! [this topic handler]
    (swap! subscriptions (fn [subs]
                           (let [orig (or (topic subs) #{})]
                             (assoc subs topic (disj orig handler))))))
  (subscribe! [this topic handler]
    (let [subscriber (Subscriber. topic handler)]
      (swap! subscriptions (fn [subs]
                             (let [clients (conj (or (topic subs) #{}) subscriber)]
                               (assoc subs topic clients))))))
  (publish! [this topic message]
    (let [subs (topic @subscriptions)]
      (doseq [sub subs]
        (proto/recv! sub this message)))))

(defn mk-event-bus
  []
  (EventBus. (atom {})))
