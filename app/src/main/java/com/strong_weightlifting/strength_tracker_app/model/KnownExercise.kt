package com.strong_weightlifting.strength_tracker_app.model

import com.strong_weightlifting.strength_tracker_app.EditTrainingActivity
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.util.*
import java.util.concurrent.atomic.AtomicLong

open class KnownExercise: RealmObject() {
    @PrimaryKey
    var uuid: Long=0
    var name: String = ""
    var user_custom_id: Int = 0
    var category: String=""
    var prWeight: Int = 1 //true max Weight lifted
    var repsAtPRWeight: Int = 1 // No of Reps at max Weight lifted
    var prCalculated: Double= 1.0 // calculated 1RM for Training Programming and Progress metric
    var dateOfPR: Date = Date()
    @LinkingObjects("knownExercise")
    val doneInExercises: RealmResults<Exercise>? = null
    var doneInExercisesSize:Int=0



    companion object{
        const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm: Realm,name:String ,user_custom_id: Int): KnownExercise? {
            val maxid:Long? =realm.where(KnownExercise::class.java).max(FIELD_UUID)?.toLong()
            maxid?.let { KnownExercise.INTEGER_COUNTER.set(it+1) }
            val knownExercise=realm.createObject(KnownExercise::class.java,increment())
            knownExercise.name=name
            knownExercise.user_custom_id=user_custom_id
            return knownExercise
        }
        fun addToExercise(realm: Realm, exerciseID : Long, knownExerciseID: Long){
            val exercise = realm.where(Exercise::class.java).equalTo(Exercise.FIELD_UUID,exerciseID).findFirst()
            exercise?.knownExercise= realm.where(KnownExercise::class.java).equalTo(FIELD_UUID,knownExerciseID).findFirst()
            exercise?.knownExercise?.doneInExercisesSize=exercise?.knownExercise?.doneInExercises?.size!!
            exercise.prWeightAtTheMoment= exercise.knownExercise!!.prWeight
            exercise.repsAtPRWeightAtTheMoment=exercise.knownExercise!!.repsAtPRWeight
            exercise.prCalculatedAtTheMoment=exercise.knownExercise!!.prCalculated

        }
        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }

        fun changeNameAndID(realm: Realm, knownExerciseUUID: Long, name: String, id: Int) {
            val knownExercise=realm.where(KnownExercise::class.java).equalTo(FIELD_UUID,knownExerciseUUID).findFirst()
            knownExercise?.name=name
            knownExercise?.user_custom_id=id
        }
        fun deleteSafely(realm: Realm,knownExerciseUUID: Long){
            val knownExercise=realm.where(KnownExercise::class.java).equalTo(FIELD_UUID,knownExerciseUUID).findFirst()
            if(knownExercise?.doneInExercisesSize==0)
                knownExercise.deleteFromRealm()
        }

    }
}
