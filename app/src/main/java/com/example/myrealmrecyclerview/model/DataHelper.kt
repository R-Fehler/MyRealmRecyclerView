package com.example.myrealmrecyclerview.model

import io.realm.Realm

class DataHelper {
    companion object{
        fun addTraining(realm: Realm){
//            val realm:Realm=Realm.getDefaultInstance()
            realm.executeTransaction { Training.create(it) }
//            realm.close()
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
            realm.executeTransaction{ KnownExercise.create(it,name,customID) }
        }

        fun addKnownExToExercise(realm:Realm, knownUUID:Long, exerciseUUID: Long){
            realm.executeTransaction { KnownExercise.addToExercise(it,exerciseUUID,knownUUID) }
        }

        fun changeKnownExercise(realm: Realm, knownExerciseUUID: Long, name: String, id: Int) {
        realm.executeTransactionAsync { KnownExercise.changeNameAndID(it,knownExerciseUUID,name,id) }
        }

        fun setNotesToTraining(realm:Realm, uuid: Long, notes:String){
            realm.executeTransactionAsync { Training.setNote(it,uuid,notes) }
        }

        //TODO delete Exer und Sets


    }
}