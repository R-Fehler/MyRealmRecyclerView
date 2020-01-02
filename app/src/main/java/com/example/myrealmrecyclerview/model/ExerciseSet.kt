package com.example.myrealmrecyclerview.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class ExerciseSet : RealmObject() {
    @PrimaryKey
    var uuid: String = UUID.randomUUID().toString()
    var weight: Int = 0
    var reps: Int = 0
    var duration: Long = 0
//reps, time, duration, ...
}
