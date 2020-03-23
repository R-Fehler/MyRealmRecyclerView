package com.strong_weightlifting.strength_tracker_app

import com.strong_weightlifting.strength_tracker_app.model.Training
import io.realm.DynamicRealm
import io.realm.RealmMigration
import java.util.*


class MyMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        var oldVersion = oldVersion

        // DynamicRealm exposes an editable schema
        val schema = realm.schema

        // Migrate to version 1: Add a new class.
        // Example:
        // open class Person(
        //     var name: String = "",
        //     var age: Int = 0,
        // ): RealmObject()
//        if (oldVersion == 0L) {
//            schema.create("Person")
//                .addField("name", String::class.java)
//                .addField("age", Int::class.javaPrimitiveType)
//            oldVersion++
//        }

        // Migrate to version 2: Add a primary key + object references
        // Example:
        // open class Person(
        //     var name: String = "",
        //     var age: Int = 0,
        //     @PrimaryKey
        //     var id: Int = 0,
        //     var favoriteDog: Dog? = null,
        //     var dogs: RealmList<Dog> = RealmList()
        // ): RealmObject()
//
//        if (oldVersion == 1L) {
////            schema.get("Person")!!
////                .addField("test", Long::class.javaPrimitiveType)
////                .addRealmObjectField("favoriteDog", schema.get("Dog"))
////                .addRealmListField("dogs", schema.get("Dog"))
//            oldVersion++
//        }
//        if (oldVersion == 2L) {
////            schema.get("Training")!!
////                .removeField("test")
////                .addRealmObjectField("favoriteDog", schema.get("Dog"))
////                .addRealmListField("dogs", schema.get("Dog"))
//            oldVersion++
//        }

        if(oldVersion==1L){
            schema.get("KnownExercise")!!
                .addField("temp_key", Double::class.javaPrimitiveType)
                .transform{
                    apply {
                        it.setDouble("temp_key",it.getInt("prCalculated").toDouble())
                    }
                }
                .removeField("prCalculated")
                .renameField("temp_key","prCalculated")

        }

        if(oldVersion==2L){
            schema.get("KnownExercise")!!
                .addField("dateOfPR", Date::class.java)

//            var prWeightAtTheMoment: Int = 0 //true max Weight lifted
//            var repsAtPRWeightAtTheMoment: Int = 0 // No of Reps at max Weight lifted
//            var prCalculatedAtTheMoment: Double= 0.0 // calculated 1RM for Training Programming and Progress metric

            schema.get("Exercise")!!
                .addField("prWeightAtTheMoment",Int::class.javaPrimitiveType)
                .addField("repsAtPRWeightAtTheMoment",Int::class.javaPrimitiveType)
                .addField("prCalculatedAtTheMoment",Double::class.javaPrimitiveType)
                oldVersion++
        }
        if(oldVersion==3L){
            schema.get("ExerciseSet")!!
                .addField("isPR",Boolean::class.javaPrimitiveType)
            oldVersion++
        }
        if(oldVersion==4L)
        {
            schema.get("MasterParent")!!
                .addRealmListField("routineList",schema.get("Training"))

        }

    }
}