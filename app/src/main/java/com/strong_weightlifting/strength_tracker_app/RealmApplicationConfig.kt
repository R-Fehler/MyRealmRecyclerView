/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.strong_weightlifting.strength_tracker_app

import android.app.Application
import com.strong_weightlifting.strength_tracker_app.model.MasterParent
import io.realm.Realm
import io.realm.RealmConfiguration


class RealmApplicationConfig : Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        val realmConfig = RealmConfiguration.Builder()
            .name("com.strong_weightlifting.strength_tracker_app.default_realm")
            .initialData { realm -> realm.createObject(MasterParent::class.java) }
            .migration(RealmMigrationSchema())
//            .deleteRealmIfMigrationNeeded()
            .schemaVersion(13)
            .allowWritesOnUiThread(true)
            .build()

//        Realm.deleteRealm(realmConfig) // Delete Realm between app restarts.
        Realm.setDefaultConfiguration(realmConfig)
    }
}
