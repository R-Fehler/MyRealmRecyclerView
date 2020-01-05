package com.example.myrealmrecyclerview.model

import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*
import java.util.concurrent.atomic.AtomicLong

open class ExerciseSet : RealmObject() {
    @PrimaryKey
    var uuid: Long=0
    var weight: Int = 0
    var reps: Int = 0
    var duration: Long = 0
//reps, time, duration, ...

    companion object{
        const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm: Realm, exerciseID: Long){

            val sets = realm.where(Exercise::class.java).equalTo(FIELD_UUID,exerciseID).findFirst()?.sets
            val maxid = realm.where(ExerciseSet::class.java).findAll()?.max(ExerciseSet.FIELD_UUID)?.toLong()
            maxid?.let { INTEGER_COUNTER.set(it+1) }

            val exerciseSet =realm.createObject(ExerciseSet::class.java, increment())
            sets?.add(exerciseSet)
        }
        fun delete(realm: Realm, uuid: Long){
            val exercise =realm.where(Exercise::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            exercise?.deleteFromRealm()
        }

        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }


    }
}
