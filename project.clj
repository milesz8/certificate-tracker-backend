(defproject certificate-tracker-backend "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [duct/core "0.8.0" :exclusions [integrant]]
                 [integrant "0.8.0"]
                 [duct/module.logging "0.5.0"]
                 [duct/migrator.ragtime "0.3.2"]
                 [duct/server.http.jetty "0.2.1"]
                 [me.grison/duct-mongodb "0.1.1" :exclusions [com.novemberain/monger]]
                 [com.novemberain/monger "3.5.0"]
                 [com.walmartlabs/lacinia "0.37.0"]
                 [metosin/reitit "0.4.2"]
                 [threatgrid/ring-graphql-ui "0.1.3"]]
  :plugins [[duct/lein-duct "0.12.1"]]
  :main ^:skip-aot certificate-tracker-backend.main
  :source-paths ["src/main/clj"]
  :test-paths ["src/test/clj"]
  :resource-paths ["src/main/resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :middleware     [lein-duct.plugin/middleware]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["src/dev/clj"]
                  :resource-paths ["src/dev/resources"]
                  :dependencies   [[com.gearswithingears/shrubbery "0.4.1"]
                                   [eftest "0.5.9"]
                                   [integrant/repl "0.3.1"]
                                   [hawk "0.2.11"]
                                   [org.testcontainers/mongodb "1.14.3"]
                                   [ring/ring-mock "0.4.0"]]
                  :plugins [[lein-eftest "0.5.9"]]}})
