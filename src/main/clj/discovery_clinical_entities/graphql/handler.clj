(ns certificate-tracker-backend.graphql.handler
  (:require [clojure.edn :as edn]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [certificate-tracker-backend.graphql.scalar-transformers :as transformers]
            [integrant.core :as ig]
            [taoensso.timbre :as log]))

(defn handler
  [executor-fn request]
  (let [{:keys [query variables]} (get-in request [:parameters :body])]
    {:status 200
     :body (executor-fn query variables nil)}))

(defmethod ig/init-key :certificate-tracker-backend.graphql/handler
  [_ {:keys [schema resolvers]}]
  (log/info ::initializing)
  (let [compiled-schema (-> schema
                            slurp
                            edn/read-string
                            (util/attach-scalar-transformers transformers/transforms)
                            (util/attach-resolvers resolvers)
                            schema/compile)
        executor-fn (partial lacinia/execute compiled-schema)]
    (partial handler executor-fn)))
