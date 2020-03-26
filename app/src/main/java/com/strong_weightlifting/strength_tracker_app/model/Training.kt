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
    var isRoutine: Boolean=false
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
        fun createRoutine(realm: Realm):Training?{
            val training=create(realm)
            training?.isRoutine=true
            return training
        }

        /**
         * Returns true if Routine was created or updated, if name =="" ot returns false -->error msg. "no name"
         */
        fun createAsRoutine(realm:Realm,training: Training):Boolean{
            val masterParent = realm.where(MasterParent::class.java).findFirst()
            val allRoutines= realm.where(Training::class.java).equalTo("isRoutine",true).findAll()
            val routine = allRoutines.find { it.name==training.name}
            if(routine!=null) {
                updateRoutine(realm,training)
                return true
                }
            else {
                if (training.name != "") {
                    val newRoutineTraining = createRoutine(realm)
                    newRoutineTraining?.name = training.name
                    training.exercises.forEachIndexed { index, exercise ->
                        createAndCopyFields(realm,index, exercise, newRoutineTraining)

                    }

                    return true
                }
                return false
            }
        }

        fun createCopy(realm: Realm,training: Training): Training? {
            val newTraining = create(realm)
            newTraining?.name = training.name
            training.exercises.forEachIndexed { index, exercise ->
                createAndCopyFields(realm,index, exercise, newTraining)

            }
            return newTraining
        }



        private fun createAndCopyFields(realm: Realm, index: Int, exercise: Exercise, newTraining: Training?) {
            var exID:Long=0
            newTraining?.uuid?.let { exID=Exercise.create(realm, it) }
            newTraining?.exercises?.get(index)?.knownExercise=exercise.knownExercise
            newTraining?.exercises?.get(index)?.prCalculatedAtTheMoment= exercise.knownExercise?.prCalculated!!
            newTraining?.exercises?.get(index)?.prWeightAtTheMoment=exercise.knownExercise?.prWeight!!
            newTraining?.exercises?.get(index)?.repsAtPRWeightAtTheMoment=exercise.knownExercise?.repsAtPRWeight!!
            exercise.sets.forEachIndexed { index, exerciseSet ->
                val newSet=ExerciseSet.create(realm,exID)
                newSet?.weight=exerciseSet.weight
                newSet?.reps=exerciseSet.reps
            }
        }

        /**
         * Returns true if Routine with same name was found and was updated
         */
        fun updateRoutine(realm:Realm,training: Training):Boolean{
            val masterParent = realm.where(MasterParent::class.java).findFirst()
            val allRoutines= realm.where(Training::class.java).equalTo("isRoutine",true).findAll()
            val routine =allRoutines.find { it.name==training.name }
            if(routine!=null) {
                for (exercise in routine.exercises) {
                    exercise.sets.deleteAllFromRealm()
                }
                routine.exercises.deleteAllFromRealm()

                training.exercises.forEachIndexed { index, exercise ->
                    var exID: Long = 0L
                    routine.uuid.let { exID = Exercise.create(realm, it) }
                    routine.exercises.get(index)?.knownExercise = exercise.knownExercise
                    exercise.sets.forEachIndexed { index, exerciseSet ->
                        val newSet = ExerciseSet.create(realm, exID)
                        newSet?.weight = exerciseSet.weight
                        newSet?.reps = exerciseSet.reps
                    }
                }
                return true
            }
            return false
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