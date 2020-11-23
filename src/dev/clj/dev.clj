(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [fipp.edn :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [duct.core :as duct]
            [duct.core.repl :as duct-repl]
            [eftest.runner :as eftest]
            [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep reset]]
            [integrant.repl.state :refer [config system]])
  (:import (org.testcontainers.containers MongoDBContainer)))

(defmethod ig/init-key :certificate-tracker-backend.testcontainers/mongodb
  [_ {:keys [database]}]
  (let [mongodb (doto (MongoDBContainer. "mongo:4.0")
                  (.start))
        host (.getHost mongodb)
        port (.getFirstMappedPort mongodb)]
  {:mongodb mongodb
   :uri (str "mongodb://" host ":" port "/" database)}))

(defmethod ig/resolve-key :certificate-tracker-backend.testcontainers/mongodb
  [_ config]
  (dissoc config :mongodb))

(defmethod ig/halt-key! :certificate-tracker-backend.testcontainers/mongodb
  [_ {:keys [mongodb]}]
  (.stop mongodb))

(duct/load-hierarchy)

(defn read-config []
  (duct/read-config (io/resource "certificate_tracker_backend/config.edn")))

(def profiles
  [:duct.profile/dev :duct.profile/local])

(clojure.tools.namespace.repl/set-refresh-dirs "src/main/clj" "src/dev/clj")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
