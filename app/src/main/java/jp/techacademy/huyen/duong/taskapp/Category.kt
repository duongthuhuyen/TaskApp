package jp.techacademy.huyen.duong.taskapp

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class Category: RealmObject, java.io.Serializable {
    @PrimaryKey
    var id = 0
    var categoryName = ""
}