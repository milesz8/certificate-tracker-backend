(ns certificate-tracker-backend.handler
  (:require [clojure.spec.alpha :as s]
            [integrant.core :as ig]
            [muuntaja.core :as m]
            [reitit.coercion :as coercion]
            [reitit.coercion.spec :as reitit-spec]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as ring-coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring-graphql-ui.core :refer [wrap-graphiql wrap-voyager wrap-playground]]
            [taoensso.timbre :as log]))

(s/def ::query string?)
(s/def ::variables (s/nilable map?))
(s/def ::graphql-request (s/keys :req-un [::query]
                                 :opt-un [::variables]))

(s/def ::data map?)
(s/def ::graphql-response (s/keys :opt-un [::data]))

(defn not-found-handler
  [request]
  (log/error ::404 request)
  {:status 404 :body "Not found" :headers {}})

(defn method-not-allowed-handler
  [request]
  (log/error ::405 request)
  {:status 405 :body "Not allowed" :headers {}})

(defn exception-handler
  [exception request]
  (log/error exception ::500 request)
  {:status 500 :body "Server error" :headers {}})

(def exception-middleware
  (exception/create-exception-middleware
    (merge exception/default-handlers
           {::exception/wrap
            (fn [handler exception request]
              (log/error exception ::500 request)
              (handler exception request))
            ::coercion/request-coercion
            (fn [exception request]
              (log/error exception ::400 request)
              {:status 400 :body "Malformed request" :headers {}})
            ::coercion/response-coercion
            (fn [exception request]
              (log/error exception ::500 request)
              {:status 500 :body "Response could not be coerced to requirements" :headers {}})})))

(defmethod ig/init-key :certificate-tracker-backend/handler
  [_ {:keys [graphql-handler]}]
  (log/info ::initializing)
  (-> (ring/ring-handler
        (ring/router [["/swagger.json"
                       {:get {:no-doc true
                              :swagger {:info {:title "Discovery Clinical Entities API"}}
                              :handler (swagger/create-swagger-handler)}}]
                      ["/graphql"
                       {:post {:summary "Endpoint for the Discovery Clinical Entities GraphQL service"
                               :parameters {:body ::graphql-request}
                               :responses {200 {:body ::graphql-response}}
                               :handler graphql-handler}}]]
                     {:data {:coercion reitit-spec/coercion
                             :muuntaja m/instance
                             :middleware [swagger/swagger-feature
                                          parameters/parameters-middleware
                                          muuntaja/format-negotiate-middleware
                                          muuntaja/format-response-middleware
                                          exception-middleware
                                          muuntaja/format-request-middleware
                                          ring-coercion/coerce-request-middleware
                                          ring-coercion/coerce-response-middleware]}})
        (ring/routes
          (swagger-ui/create-swagger-ui-handler {:path "/"})
          (ring/create-default-handler {:not-found not-found-handler
                                        :method-not-allowed method-not-allowed-handler})))
      (wrap-graphiql {:endpoint "/graphql"})
      (wrap-voyager {:endpoint "/graphql"})
      (wrap-playground {:endpoint "/graphql"})))
