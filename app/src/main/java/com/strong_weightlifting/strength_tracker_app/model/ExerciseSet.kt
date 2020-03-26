package com.strong_weightlifting.strength_tracker_app.model

import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.util.concurrent.atomic.AtomicLong

open class ExerciseSet : RealmObject() {
    @PrimaryKey
    var uuid: Long=0
    var orderNumber=1
    var weight: Int = 0 // done
    var weightPercentOf1RM: Int =0
    var reps: Int = 0 // done
    var weightPlanned: Int=0
    var repsPlanned: Int=0
    var duration: Long = 0
    var isPR: Boolean =false
    var isDone: Boolean = false
    var isWarmUp: Boolean = false
    var isDropSet: Boolean = false
    var isCompetition: Boolean = false
    var unit: String = "kg"
    @LinkingObjects("sets")
    val doneInExercises: RealmResults<Exercise>? = null
    //reps, time, duration, ...

    companion object{
        const val FIELD_UUID="uuid"
        private val INTEGER_COUNTER = AtomicLong(0)

        fun create(realm: Realm, exerciseID: Long): ExerciseSet? {
            val exercise=realm.where(Exercise::class.java).equalTo(FIELD_UUID,exerciseID).findFirst()
            val sets = exercise?.sets
            val known=realm.where(KnownExercise::class.java).equalTo(FIELD_UUID,exercise?.knownExercise?.uuid).findFirst()
            val prevExercise=known?.doneInExercises?.dropLast(1)?.maxBy { it.date }
            val prevExerciseRecord = known?.doneInExercises?.dropLast(1)?.maxBy { it.date }
            val maxid = realm.where(ExerciseSet::class.java).findAll()?.max(ExerciseSet.FIELD_UUID)?.toLong()
            maxid?.let { INTEGER_COUNTER.set(it+1) }

            val exerciseSet =realm.createObject(ExerciseSet::class.java, increment())
            // Logik im zusammenhang mit vorherigen Sets der Exercise
            val firstSetLastKnownEx= if(prevExercise?.sets?.isNotEmpty() == true)prevExercise?.sets?.first() else null

            if(sets?.size!! >0){
                exerciseSet.orderNumber= sets.size+1

                exerciseSet.weight=sets.last()?.weight ?:20
                exerciseSet.reps= sets.last()?.reps ?: 10
                val lastSetWithSameOrderNo=prevExercise?.sets?.find { it.orderNumber==exerciseSet.orderNumber }
                exerciseSet.weightPlanned=lastSetWithSameOrderNo?.weight ?: 0
                exerciseSet.repsPlanned=lastSetWithSameOrderNo?.reps ?: 0
            }

            else{
                exerciseSet.orderNumber= 1
                exerciseSet.weight= firstSetLastKnownEx?.weight ?:20
                exerciseSet.reps= firstSetLastKnownEx?.reps ?:10
                exerciseSet.weightPlanned=firstSetLastKnownEx?.weight ?:0
                exerciseSet.repsPlanned=firstSetLastKnownEx?.reps ?: 0
            }

            sets.add(exerciseSet)
            return exerciseSet
        }

        fun createWithoutAdd(realm: Realm,exerciseID: Long): ExerciseSet? {
            val exercise=realm.where(Exercise::class.java).equalTo(FIELD_UUID,exerciseID).findFirst()
            val sets = exercise?.sets
            val known=realm.where(KnownExercise::class.java).equalTo(FIELD_UUID,exercise?.knownExercise?.uuid).findFirst()
            val prevExercise=known?.doneInExercises?.dropLast(1)?.maxBy { it.date }

            val maxid = realm.where(ExerciseSet::class.java).findAll()?.max(ExerciseSet.FIELD_UUID)?.toLong()
            maxid?.let { INTEGER_COUNTER.set(it+1) }

            val exerciseSet =realm.createObject(ExerciseSet::class.java, increment())
            // Logik im zusammenhang mit vorherigen Sets der Exercise
            val firstSetLastKnownEx= if(prevExercise?.sets?.isNotEmpty() == true)prevExercise?.sets?.first() else null

            if(sets?.size!! >0){
                exerciseSet.orderNumber= sets.size+1

                exerciseSet.weight=sets.last()?.weight ?:20
                exerciseSet.reps= sets.last()?.reps ?: 10
                val lastSetWithSameOrderNo=prevExercise?.sets?.find { it.orderNumber==exerciseSet.orderNumber }
                exerciseSet.weightPlanned=lastSetWithSameOrderNo?.weight ?: 0
                exerciseSet.repsPlanned=lastSetWithSameOrderNo?.reps ?: 0
            }

            else{
                exerciseSet.orderNumber= 1
                exerciseSet.weight= firstSetLastKnownEx?.weight ?:20
                exerciseSet.reps= firstSetLastKnownEx?.reps ?:10
                exerciseSet.weightPlanned=firstSetLastKnownEx?.weight ?:0
                exerciseSet.repsPlanned=firstSetLastKnownEx?.reps ?: 0
            }

            return exerciseSet
        }

        fun delete(realm: Realm, uuid: Long){
            val exerciseSet =realm.where(ExerciseSet::class.java).equalTo(FIELD_UUID,uuid).findFirst()
            exerciseSet?.deleteFromRealm()
        }

        fun epleyValue(exerciseSet: ExerciseSet): Double {
            var fixedWeight=0.0
            if(exerciseSet.reps<1){
                return 0.0
            }
            if(exerciseSet.reps==1){
                return exerciseSet.weight.toDouble()
            }
            if(exerciseSet.weight==0)
                fixedWeight=1.0
            else
                fixedWeight=exerciseSet.weight.toDouble()

            return  fixedWeight * (1.0 + (exerciseSet.reps.toDouble() / 30.0))
        }
        fun epleyValue(weight:Int, reps:Int): Double {
            var fixedWeight=0.0
            if(reps<1){
                return 0.0
            }
            if(reps==1){
                return weight.toDouble()
            }
            if(weight==0)
                fixedWeight=1.0
            else
                fixedWeight=weight.toDouble()

            return  fixedWeight * (1.0 + (reps.toDouble() / 30.0))
        }



        private fun increment(): Long {
            return INTEGER_COUNTER.getAndIncrement()
        }


    }

    override fun toString(): String {
        if(this.isDone){
            return "${weight} $unit/ $reps; "
        }
        else{
            return "(${weight} $unit/ $reps); "
        }
    }
}
