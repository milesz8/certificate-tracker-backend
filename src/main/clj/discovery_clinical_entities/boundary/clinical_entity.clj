(ns certificate-tracker-backend.boundary.clinical-entity
  (:require [duct.database.mongodb.monger]
            [monger.collection :as mc]
            [monger.result :as mr])
  (:import (duct.database.mongodb.monger Boundary)
           (org.bson.types ObjectId)))

(defprotocol ClinicalEntitiesDatabase
  (insert-clinical-entity!
   [db clinical-entity]
   "Inserts a ClinicalEntity object into the database, returning the entity inserted.")
  (insert-coding-by-id!
   [db _id new-coding]
   "Inserts a Coding object into the a ClinicalEntity's list of codings for the given _id.")
  (update-archived-by-id!
   [db _id new-archived-value]
   "Updates the archived status of a ClinicalEntity object in the database.")
  (delete-clinical-entity-by-id!
   [db _id]
   "Removes the clinical entity document associated with the given _id.")
  (get-clinical-entities
   [db]
   "Selects all ClinicalEntity objects from the database."))

(extend-protocol ClinicalEntitiesDatabase
  Boundary
  (insert-clinical-entity! [{:keys [db]} clinical-entity]
    (let [clinical-entity-with-id (assoc clinical-entity :_id (ObjectId.))]
      (mc/insert-and-return db "clinical_entities" clinical-entity-with-id)))
  (insert-coding-by-id! [{:keys [db]} _id new-coding]
    (let [result (mc/update-by-id db
                                  "clinical_entities"
                                  _id
                                  {:$push {:finding.coding new-coding}})]
      (if (mr/updated-existing? result)
        _id
        (throw (ex-info "The given coding failed to be inserted for the given _id"
                        {:_id (str _id)
                         :coding new-coding})))))
  (update-archived-by-id! [{:keys [db]} _id new-archived-value]
    (let [result (mc/update-by-id db
                                  "clinical_entities"
                                  _id
                                  {:$set {:archived new-archived-value}})]
      (if (mr/updated-existing? result)
        _id
        (throw (ex-info "A ClinicalEntity with the given _id did not exist"
                        {:_id (str _id)})))))
  (delete-clinical-entity-by-id! [{:keys [db]} _id]
    (let [clinical-entity (mc/find-map-by-id db "clinical_entities" _id)]
      (cond
        (not clinical-entity)
        (throw (ex-info "A ClinicalEntity with the given _id did not exist" 
                        {:_id (str _id)}))

        (not (:archived clinical-entity))
        (throw (ex-info "A ClinicalEntity must be archived before deletion" 
                        {:clinical-entity clinical-entity}))

        :else (do
                (mc/insert db "clinical_entities_history" clinical-entity)
                (mc/remove-by-id db "clinical_entities" _id)
                _id))))
  (get-clinical-entities [{:keys [db]}]
    (mc/find-maps db "clinical_entities")))
