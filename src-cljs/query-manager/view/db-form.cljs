(ns query-manager.view.db-form
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :as dom]
            [cljs.core.async :as async]))

;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#db-form-container.form-container
         [:div#db-form.form
          [:h2 "Database Connection Info"]
          [:table
           [:tr [:th "type"] [:td [:select#dbf-type
                                   [:option {:value "h2"} "h2"]
                                   [:option {:value "mysql"} "mysql"]
                                   [:option {:value "oracle"} "oracle"]
                                   [:option {:value "postgresql"} "postgresql"]
                                   [:option {:value "sqlserver"} "sqlserver"]]]]
           [:tr [:th "database"] [:td [:input#dbf-database {:type "text"}]]]
           [:tr [:th "user"] [:td [:input#dbf-user {:type "text"}]]]
           [:tr [:th "password"] [:td [:input#dbf-pass {:type "password"}]]]
           [:tr [:th "host"] [:td [:input#dbf-host {:type "text"}]]]
           [:tr [:th "port"] [:td [:input#dbf-port {:type "text"}]]]]
          [:div.form-message ""]
          [:div.form-buttons
           [:button#dbf-save "save"]
           [:button#dbf-test "test"]
           [:button#dbf-cancel "cancel"]]]]))

(defn- mk-db
  []
  {:type (dom/value (sel1 :#dbf-type))
   :host (dom/value (sel1 :#dbf-host))
   :port (dom/value (sel1 :#dbf-port))
   :user (dom/value (sel1 :#dbf-user))
   :password (dom/value (sel1 :#dbf-pass))
   :database (dom/value (sel1 :#dbf-database))})

(defn- set-class!
  [sel class]
  (-> sel
      (dom/remove-class! "all-systems-go")
      (dom/remove-class! "snafu")
      (dom/remove-class! "in-progress")
      (dom/add-class! class)))

(defn- toggle-buttons!
  [state]
  (case state
    :off (do (dom/set-attr! (sel1 :#dbf-save) :disabled)
             (dom/set-attr! (sel1 :#dbf-test) :disabled))
    (do (dom/remove-attr! (sel1 :#dbf-save) :disabled)
        (dom/remove-attr! (sel1 :#dbf-test) :disabled))))

(defn- on-save!
  [output-ch]
  (fn [e]
    (async/put! output-ch [:db-save {:value (mk-db)}])
    (async/put! output-ch [:db-form-hide {}])))

(defn- on-test!
  [output-ch]
  (fn [e]
    (-> (sel1 :.form-message)
        (dom/set-html! "Testing...")
        (set-class! "in-progress"))
    (toggle-buttons! :off)
    (async/put! output-ch [:db-test {:value (mk-db)}])))

(defn- on-cancel!
  [output-ch]
  (fn [e]
    (async/put! output-ch [:db-form-hide {}])
    (async/put! output-ch [:db-poke {}])))

(defn- mk-template
  [output-ch]
  (let [t (template)]
    (dom/listen! [t :#dbf-save] :click (on-save! output-ch))
    (dom/listen! [t :#dbf-test] :click (on-test! output-ch))
    (dom/listen! [t :#dbf-cancel] :click (on-cancel! output-ch))
    t))

;;-----------------------------------------------------------------------------

(defn set-values!
  [{:keys [type host port user database password] :as db}]
  (when-let [place (sel1 :#db-form-container)]
    (dom/set-value! (sel1 :#dbf-type) type)
    (dom/set-value! (sel1 :#dbf-host) host)
    (dom/set-value! (sel1 :#dbf-user) user)
    (dom/set-value! (sel1 :#dbf-pass) password)
    (dom/set-value! (sel1 :#dbf-port) port)
    (dom/set-value! (sel1 :#dbf-database) database)))

(defn set-test-result!
  [{:keys [okay reason] :as result}]
  (toggle-buttons! :on)
  (if okay
    (-> (sel1 :.form-message)
        (dom/set-html! "Database is reachable.")
        (set-class! "all-systems-go"))
    (-> (sel1 :.form-message)
        (dom/set-html! (str "Database is unreachable: " reason))
        (set-class! "snafu"))))

(defn show!
  [queue]
  (let [body (mk-template queue)]
    (dom/append! (sel1 :body) body)))

(defn hide!
  []
  (when-let [place (sel1 :#db-form-container)]
    (dom/remove! place)))
