package com.example.myrealmrecyclerview.model

import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import io.realm.kotlin.createObject
import java.util.*
import java.util.concurrent.atomic.AtomicLong

open class KnownExercise: RealmObject() {
    @PrimaryKey
    var uuid: Long=0
    var name: String = ""
    var id: Int = 0
    var category: String=""
    var prWeight: Int = 0
    var repsAtPRWeight: Int = 0
    var prCalculated: Int= 0
    @LinkingObjects("knownExercise")
    val doneInExercises: RealmResults<Exercise>?=null



    companion object{
        const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm: Realm){
            val maxid:Long? =realm.where(KnownExercise::class.java).max(FIELD_UUID)?.toLong()
            maxid?.let { KnownExercise.INTEGER_COUNTER.set(it+1) }
            realm.createObject(KnownExercise::class.java,increment())

        }
        fun addToExercise(realm: Realm, exerciseID : Long, knownExerciseID: Long){
            val exercise = realm.where(Exercise::class.java).equalTo(Exercise.FIELD_UUID,exerciseID).findFirst()
            exercise?.knownExercise= realm.where(KnownExercise::class.java).equalTo(KnownExercise.FIELD_UUID,knownExerciseID).findFirst()

        }
        fun delete(realm: Realm, uuid: Long){
            val knownExercise =realm.where(KnownExercise::class.java).equalTo(KnownExercise.FIELD_UUID,uuid).findFirst()
            knownExercise?.deleteFromRealm()
        }
        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }
    }
}
