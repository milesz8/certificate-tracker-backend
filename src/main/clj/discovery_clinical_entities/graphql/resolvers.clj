(ns certificate-tracker-backend.graphql.resolvers
  (:require [com.walmartlabs.lacinia.resolve :as resolver]
            [certificate-tracker-backend.boundary.clinical-entity :as clinical-entity]
            [integrant.core :as ig]
            [taoensso.timbre :as log])
  (:import (java.time OffsetDateTime)
           (java.time.format DateTimeFormatter)))

(defn wrap-resolver-exceptions
  "Utility function taking a resolver and producing a wrapped resolver which on
  exceptions produces an appropriate Lacinia ResolverResult that contains the
  error information. The exception and the full resolver context are logged."
  [resolver]
  (fn [context args value]
    (try (resolver context args value)
         (catch Exception exception
           (log/error exception {:context context :args args :value value})
           (let [{:keys [message] :or {message (str exception)}} (ex-data exception)]
             (resolver/resolve-as nil {:message message}))))))

(defn get-clinical-entities
  [datastore _ _ _]
  (clinical-entity/get-clinical-entities datastore))

(defn get-finding
  [_ _ value]
  (:finding value))

(defn get-coding
  [_ _ value]
  (:coding value))

(defn register-clinical-entity!
  [datastore _ {:keys [clinicalEntity]} _]
  (let [date-time-now (.. (OffsetDateTime/now) 
                          (format (DateTimeFormatter/ISO_OFFSET_DATE_TIME)))
        clinical-entity-with-defaults (merge {:version date-time-now :archived false}
                                             clinicalEntity)]
    (clinical-entity/insert-clinical-entity! datastore clinical-entity-with-defaults)))

(defn update-archived-by-id!
  [datastore _ {:keys [_id newArchivedValue]} _]
  (clinical-entity/update-archived-by-id! datastore _id newArchivedValue))

(defn delete-clinical-entity-by-id!
  [datastore _ {:keys [_id]} _]
  (clinical-entity/delete-clinical-entity-by-id! datastore _id))

(defn register-coding-by-id!
  [datastore _ {:keys [_id newCoding]} _]
  (clinical-entity/insert-coding-by-id! datastore _id newCoding))

(defmethod ig/init-key :certificate-tracker-backend.graphql/resolvers
  [_ {:keys [datastore]}]
  (log/info ::initializing)
  (let [resolvers
        {:query/clinical-entities (partial get-clinical-entities datastore)
         :mutation/register-clinical-entity (partial register-clinical-entity! datastore)
         :mutation/register-coding-by-id (partial register-coding-by-id! datastore)
         :mutation/update-archived-by-id (partial update-archived-by-id! datastore)
         :mutation/delete-clinical-entity-by-id (partial delete-clinical-entity-by-id! datastore)
         :CodeableConcept/coding get-coding
         :ClinicalEntity/finding get-finding}]
    (reduce-kv (fn [m k v] (assoc m k (wrap-resolver-exceptions v)))
               (empty resolvers)
               resolvers)))
