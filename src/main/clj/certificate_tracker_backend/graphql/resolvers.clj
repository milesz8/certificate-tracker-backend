(ns certificate-tracker-backend.graphql.resolvers
  (:require [com.walmartlabs.lacinia.resolve :as resolver]
            [certificate-tracker-backend.boundary.home :as home]
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

(defn get-homes
  [datastore _ _ _]
  (home/get-homes datastore))

(defn register-home!
  [datastore _ {:keys [home]} _]
  (let [date-time-now (.. (OffsetDateTime/now) 
                          (format (DateTimeFormatter/ISO_OFFSET_DATE_TIME)))
        home-with-defaults (merge {:version date-time-now :archived false}
                                             home)]
    (home/insert-home! datastore home-with-defaults)))

(defn update-archived-by-id!
  [datastore _ {:keys [_id newArchivedValue]} _]
  (home/update-archived-by-id! datastore _id newArchivedValue))

(defn delete-home-by-id!
  [datastore _ {:keys [_id]} _]
  (home/delete-home-by-id! datastore _id))

(defmethod ig/init-key :certificate-tracker-backend.graphql/resolvers
  [_ {:keys [datastore]}]
  (log/info ::initializing)
  (let [resolvers
        {:query/homes (partial get-homes datastore)
         :mutation/register-home (partial register-home! datastore)
         :mutation/update-archived-by-id (partial update-archived-by-id! datastore)
         :mutation/delete-home-by-id (partial delete-home-by-id! datastore)}]
    (reduce-kv (fn [m k v] (assoc m k (wrap-resolver-exceptions v)))
               (empty resolvers)
               resolvers)))
