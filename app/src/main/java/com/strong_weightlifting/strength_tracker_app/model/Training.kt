package com.strong_weightlifting.strength_tracker_app.model

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*
import java.util.concurrent.atomic.AtomicLong

open class Training : RealmObject() {
    @PrimaryKey
    var uuid:Long=0
    var notes: String = ""
    var name: String = ""
    var date: Date = Date()
    var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    var month: Int = Calendar.getInstance().get(Calendar.MONTH)
    //duration, location, time, blabla
    var exercises: RealmList<Exercise> = RealmList()
    var duration: Long = 0 // in minutes
    var tonnage=0.0
    var isDone:Boolean=false

    companion object{
         const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm:Realm): Training? {
            val masterParent = realm.where(MasterParent::class.java).findFirst()
            val trainings: RealmList<Training>? = masterParent?.trainingList
            val maxid = realm.where(Training::class.java).findAll()?.max(FIELD_UUID)?.toLong()

            maxid?.let { INTEGER_COUNTER.set(it+1) }

            val training =realm.createObject(Training::class.java, increment())
            trainings?.add(training)
            return training

        }

        fun createCopy(realm: Realm,training: Training){
            val newTraining=create(realm)
            newTraining?.name=training.name
            for (exercise in training.exercises){

            }
            training.exercises.forEachIndexed { index, exercise ->
                var exID:Long=0
                newTraining?.uuid?.let { exID=Exercise.create(realm, it) }
                newTraining?.exercises?.get(index)?.knownExercise=exercise.knownExercise
                exercise.sets.forEachIndexed { index, exerciseSet ->
                    val newSet=ExerciseSet.create(realm,exID)
                    newSet?.weight=exerciseSet.weight
                    newSet?.reps=exerciseSet.reps
                }
            }
        }
        fun delete(realm: Realm, uuid: Long){
            val training=realm.where(Training::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            for(exercise in training?.exercises!!){
                exercise.sets.deleteAllFromRealm()
            }
            training.exercises.deleteAllFromRealm()
            training.deleteFromRealm()
        }

        fun setNote(realm:Realm, uuid: Long, notes:String){
            val training=realm.where(Training::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            training?.notes=notes
        }

        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }


    }


}