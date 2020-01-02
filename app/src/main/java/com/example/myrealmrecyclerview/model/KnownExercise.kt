package com.example.myrealmrecyclerview.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class KnownExercise: RealmObject() {
    @PrimaryKey
    var uuid=UUID.randomUUID().toString()
    var name: String = ""
    var id: Int = 0
}
