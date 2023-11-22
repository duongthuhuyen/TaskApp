package jp.techacademy.huyen.duong.taskapp

import jp.techacademy.huyen.duong.taskapp.R.*
import android.app.*
import android.app.AlarmManager.AlarmClockInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import jp.techacademy.huyen.duong.taskapp.databinding.ActivityInputBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class InputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputBinding

    private lateinit var realm: Realm
    private lateinit var task: Task
    private var selected: Int = -1
    private var category: Category = Category()
    private lateinit var listCategories: List<Category>
    private var listLabel = mutableListOf<String>()
    private lateinit var button: Button
    private lateinit var editText: EditText
    private lateinit var dialog: AlertDialog
    private var calendar = Calendar.getInstance(Locale.JAPANESE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // アクションバーの設定
        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        // ボタンのイベントリスナーの設定
        binding.content.dateButton.setOnClickListener(dateClickListener)
        binding.content.timeButton.setOnClickListener(timeClickListener)
        binding.content.doneButton.setOnClickListener(doneClickListener)

        // EXTRA_TASKからTaskのidを取得
        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)

        // Realmデータベースとの接続を開く
        val config = RealmConfiguration.create(schema = setOf(Task::class, Category::class))
        realm = Realm.open(config)

        initTask(taskId)
        // タスクを取得または初期化
        binding.content.categoryButton.setOnClickListener() {
            // category 取得
            showDialog()
            button.setOnClickListener() {
                addCategory(editText.text.toString())
                dialog.cancel()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Realmデータベースとの接続を閉じる
        realm.close()
    }

    /**
     * 日付選択ボタン
     */
    private val dateClickListener = View.OnClickListener {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                setDateTimeButtonText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * 時刻選択ボタン
     */
    private val timeClickListener = View.OnClickListener {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                setDateTimeButtonText()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        )
        timePickerDialog.show()
    }

    /**
     * 決定ボタン
     */
    private val doneClickListener = View.OnClickListener {
        CoroutineScope(Dispatchers.Default).launch {
            addTask()
            finish()
        }
    }

    /**
     * タスクを取得または初期化
     */
    private fun initTask(taskId: Int) {
        // 引数のtaskIdに合致するタスクを検索
        val findTask = realm.query<Task>("id==$taskId").first().find()

        if (findTask == null) {
            // 新規作成の場合
            task = Task()
            task.id = -1

            // 日付の初期値を1日後に設定
            calendar.add(Calendar.DAY_OF_MONTH, 1)

        } else {
            // 更新の場合
            task = findTask
            var ca = realm.query<Category>("id =${task.category}").find().first()
            // taskの日時をcalendarに反映
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)
            calendar.time = simpleDateFormat.parse(task.date) as Date

            // taskの値を画面項目に反映
            binding.content.titleEditText.setText(task.title)
            binding.content.contentEditText.setText(task.contents)
            binding.content.categoryText.setText(ca.categoryName)
        }

        // 日付と時刻のボタンの表示を設定
        setDateTimeButtonText()
    }

    /**
     * タスクの登録または更新を行う
     */
    private suspend fun addTask() {
        // 日付型オブジェクトを文字列に変換用
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)

        // 登録（更新）する値を取得
        val title = binding.content.titleEditText.text.toString()
        val content = binding.content.contentEditText.text.toString()
        val date = simpleDateFormat.format(calendar.time)

        if (task.id == -1) {
            // 登録

            // 最大のid+1をセット
            task.id = (realm.query<Task>().max("id", Int::class).find() ?: -1) + 1
            // 画面項目の値で更新
            task.title = title
            task.contents = content
            task.date = date
            task.category = listCategories[selected].id

            // 登録処理

            realm.writeBlocking {
                copyToRealm(task)
            }

        } else {
            //val ca = realm.query<Category>("id = ${listCategories[selected].id}").find().first()

            // 更新
            realm.write {
                findLatest(task)?.apply {
                    // 画面項目の値で更新
                    this.title = title
                    this.contents = content
                    this.date = date
                    this.category = listCategories[selected].id
                }
            }
        }
        // タスクの日時にアラームを設定
        val intent = Intent(applicationContext, TaskAlarmReceiver::class.java)
        intent.putExtra(EXTRA_TASK, task.id)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setAlarmClock(AlarmClockInfo(calendar.timeInMillis, null), pendingIntent)
    }

    /**
     * 日付と時刻のボタンの表示を設定する
     */
    private fun setDateTimeButtonText() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPANESE)
        binding.content.dateButton.text = dateFormat.format(calendar.time)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPANESE)
        binding.content.timeButton.text = timeFormat.format(calendar.time)

    }

    /**
     * アテゴリの登録を行う
     */
    private fun addCategory(name: String) {
        Log.d("Name", name)
        if (name != null && name != "") {
            // 最大のid+1をセット
            category.id = (realm.query<Category>().max("id", Int::class).find() ?: -1) + 1
            category.categoryName = name
            // 登録処理
            realm.writeBlocking {
                copyToRealm(category)
            }
        }
    }

    private fun getLable() {
        var categories = realm.query<Category>().find()
        if (categories != null) {
            listCategories = realm.copyFromRealm(categories)
            listLabel.clear()
            if (listCategories.size > 0) {
                for (i in listCategories.indices) {
                    listLabel.add(listCategories[i].categoryName)
                }
            }
        }
    }

    private fun showDialog() {
        getLable()
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val promptView: View = inflater.inflate(R.layout.category_input, null)
        builder
            .setTitle("アテゴリ")
            .setView(promptView)

            .setPositiveButton("Add Category") { dialog, which ->
                // Do something.
                if (selected > -1) {
                    Log.d("InputText", selected.toString())
                    binding.content.categoryText.setText(listCategories[selected].categoryName)
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Cancel") { dialog, which ->
                // Do something else.
                dialog.cancel()
            }
            .setSingleChoiceItems(
                listLabel.toTypedArray(), selected,
            ) { dialog, which ->
                selected = which
            }

        dialog = builder.create()
        dialog.show()
        button = dialog.findViewById<Button>(R.id.save_button)
        editText = dialog.findViewById<EditText>(R.id.category_edit_text)
    }

    private fun findIndex(categories: List<Category>, id: Int): Int {
        for (i in categories.indices) {
            if (id == categories[i].id) {
                return i
            }
        }
        return -1
    }
}