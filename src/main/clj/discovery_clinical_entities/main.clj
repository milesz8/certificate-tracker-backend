(ns certificate-tracker-backend.main
  (:gen-class)
  (:require [duct.core :as duct]))

(duct/load-hierarchy)

(defn -main [& args]
  (let [keys     (or (duct/parse-keys args) [:duct/daemon])
        profiles [:duct.profile/prod]]
    (-> (duct/resource "discovery_clinical_entities/config.edn")
        (duct/read-config)
        (duct/exec-config profiles keys))))
