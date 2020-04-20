package com.strong_weightlifting.strength_tracker_app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_edit_text.*

class EditTextActivity : AppCompatActivity() {
    private var realm: Realm?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()

        setContentView(R.layout.activity_edit_text)
        val notes=intent.getStringExtra(EditTrainingActivity.NOTES)
        editText.text.insert(0,notes)
        editText.selectAll()
        add_notes_btn.setOnClickListener {
            val returnIntent= Intent()
            returnIntent.putExtra(EditTrainingActivity.NOTES,editText.text.toString())
            val trainingID=intent.getLongExtra(EditTrainingActivity.TRAINING_ID,-1)
            val exerciseID=intent.getLongExtra(EditTrainingActivity.EXERCISE_ID,-1)
            returnIntent.putExtra(EditTrainingActivity.TRAINING_ID,trainingID)
            returnIntent.putExtra(EditTrainingActivity.EXERCISE_ID,exerciseID)
            setResult(Activity.RESULT_OK,returnIntent)
            finish()
        }

        clear_btn.setOnClickListener {
            editText.text.clear()
        }




    }

}
