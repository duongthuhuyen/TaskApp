package jp.techacademy.huyen.duong.taskapp

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class Task : RealmObject,java.io.Serializable{
    @PrimaryKey
    var id = 0
    var title = ""
    var contents = ""
    var date = ""
    var category = ""
    //var categoryName: Category? = null
}