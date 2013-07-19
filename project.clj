(defproject query-manager "0.2"

  :description "Submitting, monitoring and viewing the results of long-running queries."

  :dependencies [[org.clojure/clojure "1.5.1"]

                 ;; Web
                 [http-kit "2.1.6"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.3"]
                 [org.clojure/data.json "0.2.2"]

                 ;; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.13"]

                 ;; Database
                 [org.clojure/java.jdbc "0.3.0-alpha4"]    ;; jdbc
                 [mysql/mysql-connector-java "5.1.25"]     ;; mysql
                 [net.sourceforge.jtds/jtds "1.3.1"]       ;; sql-server
                 [com.h2database/h2 "1.3.172"]             ;; h2 database
                 [postgresql/postgresql "9.1-901-1.jdbc4"] ;; postgresql
                 [org.clojars.zentrope/ojdbc "11.2.0.3.0"] ;; oracle

                 ;; Services
                 [org.clojure/tools.nrepl "0.2.3"]

                 ;; ClojureScript
                 [prismatic/dommy "0.1.1"]]

  :plugins [[lein-cljsbuild "0.3.2"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds {:dev
                       {:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/qman/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}
                       :prod
                       {:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/qman/main.js"
                                   :optimizations :advanced
                                   :externs ["resources/externs.js"]
                                   :pretty-print false}}}}

  :repl-options {:port 4001}
  :min-lein-version "2.2.0"
  :main query-manager.main)
