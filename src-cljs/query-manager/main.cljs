(ns query-manager.main
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :refer [set-html! replace! replace-contents!
                                listen! append!]]
            [query-manager.net :refer [dump get-db]]))

;;-----------------------------------------------------------------------------
;; Status Bar View (Eventually)
;;-----------------------------------------------------------------------------

(defn- status-template
  []
  (node [:div#status-bar
         [:div#db-info "conn: none"]
         [:div#loading ""]
         [:div#mouse-coords
          "{:x " [:span#mouse-x "0"] " :y " [:span#mouse-y "0"] "}"]]))

(defn- set-db-info
  [{:keys [type host] :as db}]
  (replace! (sel1 :#db-info) (node [:div#db-info
                                    "conn: "
                                    [:span#db-type type]
                                    " on "
                                    [:span#db-host host]])))

;;-----------------------------------------------------------------------------

(defn- keywordize
  [m]
  (reduce (fn [a [k v]] (assoc a (keyword k) v)) {} m))

(let [c (atom 0)]
  (defn- counter
    []
    (swap! c inc)))

(defn- ni-template
  []
  (node [:div#container
         [:h1 "Query Manager App"]
         [:p "Client not yet implemented."]
         [:p [:button#test-button "Test"]]
         [:p.flash "~"]]))

(defn- loading
  [toggle]
  (if toggle
    (set-html! (sel1 :#loading) "loading...")
    (set-html! (sel1 :#loading) "")))

(defn- flash
  [msg]
  (let [m (str msg " &bull; " (counter))]
    (set-html! (sel1 :.flash) m)))

(defn- fake-action
  []
  (loading true)
  (let [data [{:id "1" :sql "select * from foo" :description "Blah"}]]

    (get-db (fn [data]
              (let [db (keywordize (js->clj data))]
                (.log js/console "data" db)
                (loading false)
                (flash (str db))
                (set-db-info db)))
            (fn [e] (flash (str "Failed: " (:status e) " -> " (:reason e)))))))

(defn- ni-view
  []
  (let [ni (ni-template)]
    (listen! [ni :#test-button] :click fake-action)
    ni))

(defn main
  []
  (.log js/console "Hello.")
  (replace-contents! (sel1 :body) (ni-view))
  (append! (sel1 :body) (status-template))

  (listen! (sel1 :body)
           :mousemove (fn [e]
                        (set-html! (sel1 :#mouse-x) (.-clientX e))
                        (set-html! (sel1 :#mouse-y) (.-clientY e))))

  (.log js/console "Goodbye."))

(set! (.-onload js/window) main)
