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



    }
}