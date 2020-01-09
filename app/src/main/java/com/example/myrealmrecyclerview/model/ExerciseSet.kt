package com.example.myrealmrecyclerview.model

import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*
import java.util.concurrent.atomic.AtomicLong

open class ExerciseSet : RealmObject() {
    @PrimaryKey
    var uuid: Long=0
    var orderNumber=1
    var weight: Int = 0 // done
    var reps: Int = 0 // done
    var weightPlanned: Int=0
    var repsPlanned: Int=0
    var duration: Long = 0
    var isDone: Boolean = false
    var isWarmUp: Boolean = false
    var isDropSet: Boolean = false
    var isCompetition: Boolean = false
//reps, time, duration, ...

    companion object{
        const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm: Realm, exerciseID: Long){

            val sets = realm.where(Exercise::class.java).equalTo(FIELD_UUID,exerciseID).findFirst()?.sets
            val maxid = realm.where(ExerciseSet::class.java).findAll()?.max(ExerciseSet.FIELD_UUID)?.toLong()
            maxid?.let { INTEGER_COUNTER.set(it+1) }

            val exerciseSet =realm.createObject(ExerciseSet::class.java, increment())
            // Logik im zusammenhang mit vorherigen Sets der Exercise
            if(sets?.size!! >0){
                exerciseSet.orderNumber= sets.size.plus(1)

                exerciseSet.repsPlanned= sets.last()?.reps ?: 5
                exerciseSet.weightPlanned=sets.last()?.weightPlanned ?:20
            }

            sets.add(exerciseSet )
        }
        fun delete(realm: Realm, uuid: Long){
            val exerciseSet =realm.where(ExerciseSet::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            exerciseSet?.deleteFromRealm()
        }



        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }


    }
}
