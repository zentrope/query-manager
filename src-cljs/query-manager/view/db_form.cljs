(ns query-manager.view.db-form
  (:use-macros [dommy.macros :only [sel1 node]])
  (:require [dommy.core :refer [set-value! value listen! show! hide!
                                remove-class! add-class! set-html!
                                set-attr! remove-attr!]]
            [query-manager.view :refer [mk-view]]
            [query-manager.protocols :refer [publish!]]))

;;-----------------------------------------------------------------------------
;; Implementation
;;-----------------------------------------------------------------------------

(defn- template
  []
  (node [:div#db-form-container.form-container {:style {:display "none"}}
         [:div#db-form.form
          [:h2 "Database Connection Info"]
          [:table
           [:tr [:th "type"] [:td [:select#dbf-type
                                   [:option {:value "h2"} "h2"]
                                   [:option {:value "mysql"} "mysql"]
                                   [:option {:value "oracle"} "oracle"]
                                   [:option {:value "sqlserver"} "sqlserver"]
                                   [:option {:value "postgresql"} "postgresql"]]]]
           [:tr [:th "database"] [:td [:input#dbf-database {:type "text"}]]]
           [:tr [:th "user"] [:td [:input#dbf-user {:type "text"}]]]
           [:tr [:th "password"] [:td [:input#dbf-pass {:type "password"}]]]
           [:tr [:th "host"] [:td [:input#dbf-host {:type "text"}]]]
           [:tr [:th "port"] [:td [:input#dbf-port {:type "text"}]]]]
          [:div.form-message
           ""]
          [:div.form-buttons
           [:button#dbf-save "save"]
           [:button#dbf-test "test"]
           [:button#dbf-cancel "cancel"]]]]))

(defn- mk-db
  []
  {:type (value (sel1 :#dbf-type))
   :host (value (sel1 :#dbf-host))
   :port (value (sel1 :#dbf-port))
   :user (value (sel1 :#dbf-user))
   :password (value (sel1 :#dbf-pass))
   :database (value (sel1 :#dbf-database))})

(defn- set-class!
  [sel class]
  (-> sel
      (remove-class! "all-systems-go")
      (remove-class! "snafu")
      (remove-class! "in-progress")
      (add-class! class)))

(defn- toggle-buttons!
  [state]
  (case state
    :off (do (set-attr! (sel1 :#dbf-save) :disabled)
             (set-attr! (sel1 :#dbf-test) :disabled))
    (do (remove-attr! (sel1 :#dbf-save) :disabled)
        (remove-attr! (sel1 :#dbf-test) :disabled))))

(defn- on-save!
  [mbus]
  (fn [e]
    (publish! mbus :db-save {:value (mk-db)})
    (publish! mbus :db-form-hide {})))

(defn- on-test!
  [mbus]
  (fn [e]
    (-> (sel1 :.form-message)
        (set-html! "Testing...")
        (set-class! "in-progress"))
    (toggle-buttons! :off)
    (publish! mbus :db-test {:value (mk-db)})))

(defn- on-cancel!
  [mbus]
  (fn [e]
    (publish! mbus :db-form-hide {})
    (publish! mbus :db-poke {})))

(defn- mk-template
  [mbus]
  (let [t (template)]
    (listen! [t :#dbf-save] :click (on-save! mbus))
    (listen! [t :#dbf-test] :click (on-test! mbus))
    (listen! [t :#dbf-cancel] :click (on-cancel! mbus))
    t))

(defn- on-db-test-result!
  [{:keys [okay reason]}]
  (toggle-buttons! :on)
  (if okay
    (-> (sel1 :.form-message)
        (set-html! "Database is reachable.")
        (set-class! "all-systems-go"))
    (-> (sel1 :.form-message)
        (set-html! (str "Database is unreachable: " reason))
        (set-class! "snafu"))))

(defn- on-update!
  [mbus {:keys [type host port user database password]}]
  (set-value! (sel1 :#dbf-type) type)
  (set-value! (sel1 :#dbf-host) host)
  (set-value! (sel1 :#dbf-user) user)
  (set-value! (sel1 :#dbf-pass) password)
  (set-value! (sel1 :#dbf-port) port)
  (set-value! (sel1 :#dbf-database) database))

(defn- on-show!
  [mbus db-info]
  (-> (sel1 :.form-message)
      (set-class! "")
      (set-html! "&nbsp;"))
  (toggle-buttons! :on)
  (show! (sel1 :#db-form-container)))

(defn- on-hide!
  [mbus db-info]
  (hide! (sel1 :#db-form-container)))

(def ^:private subscriptions
  {:db-test-result (fn [mbus msg] (on-db-test-result! (:value msg)))
   :db-change (fn [mbus msg] (on-update! mbus (:value msg)))
   :db-form-show (fn [mbus msg] (on-show! mbus (:value msg)))
   :db-form-hide (fn [mbus msg] (on-hide! mbus (:value msg)))})

;;-----------------------------------------------------------------------------
;; Interface
;;-----------------------------------------------------------------------------

(defn mk-view!
  [mbus]
  (mk-view mbus mk-template subscriptions))
