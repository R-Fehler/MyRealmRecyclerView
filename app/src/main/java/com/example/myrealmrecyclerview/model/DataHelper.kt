package com.example.myrealmrecyclerview.model

import io.realm.Realm

class DataHelper {
    companion object{
        fun addTrainingAsync(realm: Realm){
//            val realm:Realm=Realm.getDefaultInstance()
            realm.executeTransaction { Training.create(realm) }
//            realm.close()
        }
        fun deleteTrainingAsync(realm: Realm, uuid:Int){
            realm.executeTransactionAsync { Training.delete(realm,uuid) }
        }
        fun deleteTrainingsAsync(realm: Realm, uuids: Collection<Int>) {
            realm.executeTransactionAsync {
                for (uuid in uuids){
                    Training.delete(realm, uuid)
                }
            }
        }
    }
}