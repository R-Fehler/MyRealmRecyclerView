package com.example.myrealmrecyclerview.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Exercise : RealmObject() {
    @PrimaryKey
    var uuid: String = UUID.randomUUID().toString()

    var knownExercise: KnownExercise? = null

    var sets: RealmList<ExerciseSet> = RealmList()
}
