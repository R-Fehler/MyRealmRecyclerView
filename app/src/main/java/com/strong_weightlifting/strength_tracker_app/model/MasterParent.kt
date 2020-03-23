package com.strong_weightlifting.strength_tracker_app.model

import io.realm.RealmList
import io.realm.RealmObject

open class MasterParent(
    @SuppressWarnings("unused")
    var trainingList: RealmList<Training> = RealmList(),
    var routineList: RealmList<Training> = RealmList()
) : RealmObject() {
}