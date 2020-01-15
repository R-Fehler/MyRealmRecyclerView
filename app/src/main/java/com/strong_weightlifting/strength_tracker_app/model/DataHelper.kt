package com.strong_weightlifting.strength_tracker_app.model

import io.realm.Realm

class DataHelper {
    companion object{
        fun addTraining(realm: Realm): Training? {
            var training: Training?=null
            realm.executeTransaction {training = Training.create(it) }
            return training
            // TODO return training for import
//            realm.close()
        }

        fun copyTraining(realm: Realm,training: Training){
            realm.executeTransaction { Training.createCopy(it,training) }
        }
        fun deleteTraining(realm: Realm, uuid:Long){
            realm.executeTransaction { Training.delete(it,uuid) }
        }
        fun deleteTrainingsAsync(realm: Realm, uuids: Collection<Long>) {
            realm.executeTransactionAsync {
                for (uuid in uuids){
                    Training.delete(it, uuid)
                }
            }
        }
        fun addExercise(realm: Realm,trainingUUID:Long): Long {
            var primary:Long=0
            realm.executeTransaction {primary= Exercise.create(it,trainingUUID) }
            return primary
        }
        fun deleteExercise(realm: Realm,exerciseUUID: Long){
            realm.executeTransaction { Exercise.delete(it,exerciseUUID) }
        }

        fun addExerciseSet(realm: Realm, exerciseUUID:Long) {
            realm.executeTransaction { ExerciseSet.create(it,exerciseUUID) }
        }
        fun deleteExerciseSet(realm: Realm,exerciseSetUUID: Long){
            realm.executeTransaction { ExerciseSet.delete(it,exerciseSetUUID) }
        }

        fun createKnownExercise(realm: Realm, name:String, customID:Int){
            realm.executeTransaction { KnownExercise.create(it,name,customID) }
        }

        fun addKnownExToExercise(realm:Realm, knownUUID:Long, exerciseUUID: Long){
            realm.executeTransaction { KnownExercise.addToExercise(it,exerciseUUID,knownUUID) }
        }

        fun changeKnownExercise(realm: Realm, knownExerciseUUID: Long, name: String, id: Int) {
        realm.executeTransaction { KnownExercise.changeNameAndID(it,knownExerciseUUID,name,id) }
        }

        fun setNotesToTraining(realm:Realm, uuid: Long, notes:String){
            realm.executeTransaction { Training.setNote(it,uuid,notes) }
        }

        fun setNotesToExercise(realm: Realm,uuid: Long,notes:String){
            realm.executeTransaction {  Exercise.setNote(it,uuid,notes)}
        }

        //TODO delete Exer und Sets


    }
}