package jp.techacademy.huyen.duong.taskapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class TaskAdapter(context: Context) : BaseAdapter(){
    private val layoutInflater: LayoutInflater
    private var taskList = mutableListOf<Task>()
    init {
        this.layoutInflater = LayoutInflater.from(context)
    }
    override fun getCount(): Int {
        return taskList.size
    }

    override fun getItem(p0: Int): Any {
        return taskList.get(p0)
    }

    override fun getItemId(p0: Int): Long {
        return taskList[p0].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView?: layoutInflater.inflate(
            android.R.layout.simple_expandable_list_item_2,
            parent,
            false
        )
        val textView1 = view.findViewById<TextView>(android.R.id.text1)
        val textView2 = view.findViewById<TextView>(android.R.id.text2)
        textView1.text = taskList[position].title+": "+taskList[position].category
        textView2.text = taskList[position].date
        return view;
    }
    fun updateTaskList(taskList:List<Task>) {
        // 一度クリアしてから新しいタスク一覧に入替
        this.taskList.clear()
        this.taskList.addAll(taskList)
        // データに変更があったことをadapterに通知
        notifyDataSetChanged()
    }
}