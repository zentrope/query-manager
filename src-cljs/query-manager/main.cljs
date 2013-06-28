(ns query-manager.main
  ;;
  ;; enfocus
  ;;
  (:require [enfocus.core :as ef]
;;            [enfocus.events :as events]
;;            [enfocus.effects :as effects]
            [hiccups.runtime :as hiccupsrt])
  (:require-macros [enfocus.macros :as em]
                   [hiccups.core :refer [html]]))

(defn- not-implemented
  []
  (html [:h1 "Query Manager App"]
        [:p "Client not yet implemented."]))

(defn ^:export main
  []
  (.log js/console "Hello.")
  (em/at js/document ["body"]
         (em/do->
          (em/fade-out 1)
          (em/html-content (not-implemented))
          (em/fade-in 500))))

(set! (.-onload js/window) main)
