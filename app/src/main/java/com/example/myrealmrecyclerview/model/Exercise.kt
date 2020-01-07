package com.example.myrealmrecyclerview.model

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*
import java.util.concurrent.atomic.AtomicLong

open class Exercise : RealmObject() {
    @PrimaryKey
    var uuid: Long=0

    var knownExercise: KnownExercise? = null

    var sets: RealmList<ExerciseSet> = RealmList()
    companion object{
        const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm: Realm, trainingID: Long){
            val exercises = realm.where(Training::class.java).equalTo(FIELD_UUID,trainingID).findFirst()?.exercises

            val maxid = realm.where(Exercise::class.java).findAll()?.max(FIELD_UUID)?.toLong()
            maxid?.let { INTEGER_COUNTER.set(it+1) }

            val exercise =realm.createObject(Exercise::class.java, increment())
            exercises?.add(exercise)
        }
        fun delete(realm: Realm, uuid: Long){
            val exercise =realm.where(Exercise::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            exercise?.sets?.deleteAllFromRealm()
            exercise?.deleteFromRealm()
        }

        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }


    }
}
