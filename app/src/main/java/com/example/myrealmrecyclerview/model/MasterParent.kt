package com.example.myrealmrecyclerview.model

import io.realm.RealmList
import io.realm.RealmObject

open class MasterParent(
    @SuppressWarnings("unused")
    var trainingList: RealmList<Training> = RealmList()

) : RealmObject() {
}