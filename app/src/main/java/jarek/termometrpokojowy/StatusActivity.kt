package jarek.termometrpokojowy

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class StatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)
        val sb = StringBuilder()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        sb.append("Report time: " + fmt.format(Calendar.getInstance().time) + "\n")
        sb.append(intent.getStringExtra("msg"))
        findViewById<TextView>(R.id.msg1).text = sb.toString()
    }
}
