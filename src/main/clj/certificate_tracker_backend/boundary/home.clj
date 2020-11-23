(ns certificate-tracker-backend.boundary.home
  (:require [duct.database.mongodb.monger]
            [monger.collection :as mc]
            [monger.result :as mr])
  (:import (duct.database.mongodb.monger Boundary)
           (org.bson.types ObjectId)))

(defprotocol ClinicalEntitiesDatabase
  (insert-home!
   [db home]
   "Inserts a Home object into the database, returning the entity inserted.")
  (update-archived-by-id!
   [db _id new-archived-value]
   "Updates the archived status of a Home object in the database.")
  (delete-home-by-id!
   [db _id]
   "Removes the clinical entity document associated with the given _id.")
  (get-homes
   [db]
   "Selects all Home objects from the database."))

(extend-protocol ClinicalEntitiesDatabase
  Boundary
  (insert-home! [{:keys [db]} home]
    (let [home-with-id (assoc home :_id (ObjectId.))]
      (mc/insert-and-return db "homes" home-with-id)))
  (update-archived-by-id! [{:keys [db]} _id new-archived-value]
    (let [result (mc/update-by-id db
                                  "homes"
                                  _id
                                  {:$set {:archived new-archived-value}})]
      (if (mr/updated-existing? result)
        _id
        (throw (ex-info "A Home with the given _id did not exist"
                        {:_id (str _id)})))))
  (delete-home-by-id! [{:keys [db]} _id]
    (let [home (mc/find-map-by-id db "homes" _id)]
      (cond
        (not home)
        (throw (ex-info "A Home with the given _id did not exist" 
                        {:_id (str _id)}))

        (not (:archived home))
        (throw (ex-info "A Home must be archived before deletion" 
                        {:home home}))

        :else (do
                (mc/insert db "homes_history" home)
                (mc/remove-by-id db "homes" _id)
                _id))))
  (get-homes [{:keys [db]}]
    (mc/find-maps db "homes")))
