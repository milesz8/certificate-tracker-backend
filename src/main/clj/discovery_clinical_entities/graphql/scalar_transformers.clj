(ns certificate-tracker-backend.graphql.scalar-transformers
  (:import (org.bson.types ObjectId)))

(defn object-id->id
  [object-id]
  (str object-id))

(defn id->object-id
  [id]
  (ObjectId. id))

(def transforms 
  {:object-id id->object-id
   :id object-id->id})