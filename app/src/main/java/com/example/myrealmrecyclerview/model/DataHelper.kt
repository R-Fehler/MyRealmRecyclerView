package com.example.myrealmrecyclerview.model

import io.realm.Realm

class DataHelper {
    companion object{
        fun addTrainingAsync(realm: Realm){
//            val realm:Realm=Realm.getDefaultInstance()
            realm.executeTransactionAsync { Training.create(it) }
//            realm.close()
        }
        fun deleteTrainingAsync(realm: Realm, uuid:Long){
            realm.executeTransactionAsync { Training.delete(it,uuid) }
        }
        fun deleteTrainingsAsync(realm: Realm, uuids: Collection<Long>) {
            realm.executeTransactionAsync {
                for (uuid in uuids){
                    Training.delete(it, uuid)
                }
            }
        }
        fun addExerciseAsync(realm: Realm, trainingUUID:Long) {

            realm.executeTransactionAsync { Exercise.create(it,trainingUUID) }

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

        fun createKnownExerciseAsync(realm: Realm,name:String, customID:Int){
            realm.executeTransactionAsync { KnownExercise.create(it,name,customID) }
        }

        fun addKnownExToExercise(realm:Realm, knownUUID:Long, exerciseUUID: Long){
            realm.executeTransaction { KnownExercise.addToExercise(it,exerciseUUID,knownUUID) }
        }

        fun changeKnownExercise(realm: Realm, knownExerciseUUID: Long, name: String, id: Int) {
        realm.executeTransactionAsync { KnownExercise.changeNameAndID(it,knownExerciseUUID,name,id) }
        }

        //TODO delete Exer und Sets


    }
}