package com.example.myrealmrecyclerview.model

import android.util.Log
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.kotlin.createObject
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

open class Training : RealmObject() {
    @PrimaryKey
    var uuid:Long=0
    var notes: String = ""
    var name: String = ""
    var date: Date = Date()
    //duration, location, time, blabla
    var exercises: RealmList<Exercise> = RealmList()
    var duration: Long = 0 // in minutes
    var tonnage=0.0
    var isDone:Boolean=false

    companion object{
         const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm:Realm){
//            val realm=Realm.getDefaultInstance()
            val masterParent = realm.where(MasterParent::class.java).findFirst()
            val trainings: RealmList<Training>? = masterParent?.trainingList
            val maxid = realm.where(Training::class.java).findAll()?.max(FIELD_UUID)?.toLong()

            maxid?.let { INTEGER_COUNTER.set(it+1) }

            val training =realm.createObject(Training::class.java, increment())
            trainings?.add(training)
//            realm.close()
        }
        fun delete(realm: Realm, uuid: Long){
            val training=realm.where(Training::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            for(exercise in training?.exercises!!){
                exercise.sets.deleteAllFromRealm()
            }
            training.exercises.deleteAllFromRealm()
            training.deleteFromRealm()
        }

        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }


    }


}