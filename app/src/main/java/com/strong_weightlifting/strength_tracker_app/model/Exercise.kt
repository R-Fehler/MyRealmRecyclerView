package com.strong_weightlifting.strength_tracker_app.model

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.util.*
import java.util.concurrent.atomic.AtomicLong

open class Exercise : RealmObject() {
    @PrimaryKey
    var uuid: Long=0
    var notes: String=""
    var date: Date = Date()

    var knownExercise: KnownExercise? = null

    var sets: RealmList<ExerciseSet> = RealmList()
    @LinkingObjects("exercises")
    val doneInTrainings: RealmResults<Training>?=null

    companion object{
        const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm: Realm, trainingID: Long): Long {
            val exercises = realm.where(Training::class.java).equalTo(FIELD_UUID,trainingID).findFirst()?.exercises

            val maxid = realm.where(Exercise::class.java).findAll()?.max(FIELD_UUID)?.toLong()
            maxid?.let { INTEGER_COUNTER.set(it+1) }
            val primary= increment()
            val exercise =realm.createObject(Exercise::class.java, primary)
            exercises?.add(exercise)
            return primary
        }
        fun delete(realm: Realm, uuid: Long){
            val exercise =realm.where(Exercise::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            exercise?.sets?.deleteAllFromRealm()
            exercise?.deleteFromRealm()
        }

        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }

        fun setNote(realm: Realm, uuid: Long, notes:String){
            val exercise =realm.where(Exercise::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            exercise?.notes=notes
        }
    }

    override fun toString(): String {
        var text = ""
        if (sets.size>1) {
            for (n in 0..sets.size - 1) {
                text += if (n <= sets.size - 1) {

                    when {
                        n == 0 -> "${sets[n]?.weight}kg:${sets[n]?.reps}"
                        sets[n]?.weight == sets[n - 1]?.weight -> "/${sets[n]?.reps}"
                        else -> "  ${sets[n]?.weight}kg:${sets[n]?.reps}"
                    }
                }
                else ""
            }
            return "[${knownExercise?.user_custom_id}] ${knownExercise?.name} : $text"
        }
        else if(sets.size==1) {
            text += "${sets[0]?.weight}kg:${sets[0]?.reps}"
            return "[${knownExercise?.user_custom_id}] ${knownExercise?.name} : $text"
        }
        else

        return "[${knownExercise?.user_custom_id}] ${knownExercise?.name} :"
    }
}
