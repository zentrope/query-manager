(ns query-manager.main
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :refer [set-html! replace! replace-contents!
                                listen! append!]]
            [query-manager.view.status-bar :as status-bar]
            [query-manager.view.title-bar :as title-bar]
            [query-manager.net :refer [dump get-db]]))

;;-----------------------------------------------------------------------------
;; Events
;;-----------------------------------------------------------------------------

(def ^:private subscribers (atom {}))

(defn- add-sub
  [event-type f]
  (swap! subscribers (fn [s]
                       (let [orig (or (event-type s) #{})]
                         (assoc s event-type (conj orig f))))))

(defn- add-subs
  [event-types f]
  (doseq [e event-types]
    (add-sub e f)))

(defn- now
  []
  (.getTime (js/Date.)))

(defn- send-event
  [event]
  (let [topic (first event)
        data (assoc (second event) :timestamp (now))]
    (when-let [subscribers (topic @subscribers)]
      (doseq [s subscribers]
        (s event)))))

;;-----------------------------------------------------------------------------

(defn- start-clock
  []
  (js/setTimeout (fn []
                   (send-event [:clock {:value (now)}])
                   (start-clock)) 1000))

(defn- ni-template
  []
  (node [:div#container
         [:h1 "Query Manager App"]
         [:p "Client not yet implemented."]
         [:p [:button#test-button "Test"]]
         [:p.flash "~"]]))

(defn- loading
  [toggle]
  (send-event [:loading {:value toggle}]))

(defn- flash
  [msg]
  (set-html! (sel1 :.flash) msg))

(defn- fake-action
  []
  (loading true)
  (let [data [{:id "1" :sql "select * from foo" :description "Blah"}]]

    (get-db (fn [data]
              (let [db (js->clj data :keywordize-keys true)]
                (loading false)
                (send-event [:db-change {:value db}])
                (flash (str db))))
            (fn [e] (flash (str "Failed: " (:status e) " -> " (:reason e)))))))

(defn- ni-view
  []
  (let [ni (ni-template)]
    (listen! [ni :#test-button] :click fake-action)
    ni))

(defn main
  []
  (.log js/console "loading")
  (replace-contents! (sel1 :body) (title-bar/dom))
  (append! (sel1 :body) (ni-view))
  (append! (sel1 :body) (status-bar/dom))

  (listen! (sel1 :body) :mousemove
           (fn [e]
             (let [x (.-clientX e)
                   y (.-clientY e)]
               (send-event [:mousemove {:value [x y]}]))))

  (add-subs (status-bar/events) status-bar/recv)
  (add-subs (title-bar/events) title-bar/recv)

  (start-clock)

  (.log js/console "loaded"))

(set! (.-onload js/window) main)
