package jarek.termometrpokojowy

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import java.net.HttpURLConnection


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class Main : AppCompatActivity() {
    private var updateWeatherThread: Thread? = null
    private var threadWatcherTimer: Timer? = null
    private var threadWatcherLastMsg: String = "never"
    private var lastWeatherRead: Date? = null
    private val messageList = mutableListOf<String>()
    private var wakeLock: PowerManager.WakeLock? = null

    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        println("onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreen_content.setOnClickListener { toggle() }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        status_button.setOnTouchListener(mDelayHideTouchListener)

        val contentView: View = findViewById(android.R.id.content)
        contentView.post { refreshView(contentView) }

        threadWatcherTimer = Timer()
        threadWatcherTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                threadWatcherLastMsg = "active at " + time2Str(Calendar.getInstance().time)
                if (updateWeatherThread?.isAlive != true) {
                    val msg = "updateWeatherThread restarted at " +
                            time2Str(Calendar.getInstance().time)
                    addMessage(msg)
                    println("restarted thread text: " + msg)
                    updateWeatherThread = Thread { updateWeatherContinually(contentView, this@Main) }
                    updateWeatherThread?.start()
                }
            }
        }, 0L, 10000L)

        Thread { readMotionContinually(this) }.start()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "jarektermometr:main")
        println("wakeLock acquire?")
        wakeLock?.acquire()
        println("wakeLock acquire ok, state: " + wakeLock?.isHeld)
    }

    fun time2Str(d: Date): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return fmt.format(d)
    }

    fun currentTimeStr(): String {
        return time2Str(Calendar.getInstance().time)
    }

    override fun onPause() {
        println("onPause, wake lock state: " + wakeLock?.isHeld)
        addMessage("PAUSE at " + currentTimeStr()
                + " (last read "
                + if (lastWeatherRead == null) "<never>" else time2Str(lastWeatherRead!!) + ")")
        super.onPause()
    }

    fun wakeUpDevice() {
        println("will wakeUp, main lock is " + wakeLock?.isHeld)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                        + PowerManager.ACQUIRE_CAUSES_WAKEUP
                        + PowerManager.ON_AFTER_RELEASE,
                "jarektermometr:wakeup"
        )
        lock.acquire()
        println("wakeUp lock acquired, main lock is " + wakeLock?.isHeld)
        lock.release()
        println("wakeUp done, main lock is " + wakeLock?.isHeld)
    }

    override fun onResume() {
        println("onResume, wake lock state: " + wakeLock?.isHeld)
        if (wakeLock?.isHeld != true) {
            println("reacquiring wake lock")
            wakeLock?.acquire()
        }
        addMessage("RESUME at " + currentTimeStr())
        val readAgeMillis = Calendar.getInstance().timeInMillis - (lastWeatherRead?.time ?: 0)
        addMessage("age: " + (readAgeMillis / 1000) + " seconds")
        if (readAgeMillis > 4 * 60 * 1000L) {
            Thread { updateWeatherOnce(findViewById(android.R.id.content), this) }.start()
        }
        super.onResume()
    }

    override fun onStart() {
        println("onStart1, wake lock state: " + wakeLock?.isHeld)
        super.onStart()
        println("onStart2, wake lock state: " + wakeLock?.isHeld)
    }

    override fun onStop() {
        println("onStop, wake lock state: " + wakeLock?.isHeld)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy, wake lock state: " + wakeLock?.isHeld)
        if (wakeLock?.isHeld() == true)
            wakeLock?.release()
        else
            println("not releasing wakeLock, not held")
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    fun addMessage(msg: String) {
        messageList.add(msg)
        findViewById<TextView>(R.id.msg1).text = messageList.takeLast(5).joinToString("\n")
    }

    fun finishAction(view: View) {
        finish()
    }

    fun statusAction(view: View) {
        //refreshView(view)
        val intent = Intent(applicationContext, StatusActivity::class.java)
        val sb = StringBuilder()
        sb.append("weather thread active: " + updateWeatherThread?.isAlive + "\n")
        sb.append("thread watcher timer: " + threadWatcherLastMsg)
        intent.putExtra("msg", sb.toString())
        startActivity(intent)
    }

    var timer: Timer? = null

    fun refreshView(view: View) {
        val time = Calendar.getInstance().time
        val formatter = SimpleDateFormat("HH:mm:ss")
        val timeStr = formatter.format(time)
        findViewById<TextView>(R.id.time)?.setText(timeStr)
        if (timer == null) {
            // How many millis left to time change?
            val delay = 1000L - Calendar.getInstance().timeInMillis % 1000
            view.postDelayed( { refreshView(view)}, delay)
        }
    }

    private fun fillFromUrl(view: View, act: Main, urlString: String) {
        println("fillFromUrl starts")
        var weatherString = ""
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            println("connection response: " + conn.responseCode)
            if (conn.responseCode == 503) {
                // Heroku gives it after every 30 seconds.
                weatherString = ""
            } else {
                val istr = conn.getInputStream()!!
                weatherString = istr.readBytes().toString(Charset.forName("UTF-8"))
                        //.replace("°", "")
                        .replace("\n", "")
                //.replace(Regex("[NESW]+ przy "), "")
                act.lastWeatherRead = Calendar.getInstance().time
            }
        } catch (e: IOException) {
            println(e)
            e.printStackTrace()
            weatherString = e.message!!
        }
        if (!weatherString.equals("")) {
            println("fillFromUrl will post")
            view.post {
                view.findViewById<TextView>(R.id.weather).text = weatherString
                if (act.lastWeatherRead != null) {
                    val updateTime = time2Str(act.lastWeatherRead!!)
                    view.findViewById<TextView>(R.id.updateTime).text = updateTime
                }
            }
            println("fillFromUrl posted")
        }
    }

    // To be run in worker thread.
    fun updateWeatherOnce(view: View, act: Main) {
        //fillFromUrl(view, act,"http://jarek.katowice.pl/jcwww/events/read_event.php?code=weather&last")
        fillFromUrl(view, act,"https://eventserver75.herokuapp.com/events/read?code=weather&last")
    }

    // To be run in worker thread.
    fun updateWeatherContinually(view: View, act: Main) {
        updateWeatherOnce(view, act)
        while(true) {
            //fillFromUrl(view, act, "http://jarek.katowice.pl/jcwww/events/read_event.php?code=weather")
            fillFromUrl(view, act, "https://eventserver75.herokuapp.com/events/read?code=weather")
        }
    }

    // To be run in worker thread.
    fun readMotionContinually(act: Main) {
        val urlString = "https://eventserver75.herokuapp.com/events/read?code=ruch"
        var motionString = ""
        while(true) {
            println("motionReader loop begin")
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                println("motionReader connection response: " + conn.responseCode)
                if (conn.responseCode != 503) {
                    val istr = conn.inputStream!!
                    motionString = istr.readBytes().toString(Charset.forName("UTF-8"))
                            .replace("\n", "")
                    istr.close()
                } else {
                    motionString = ""
                }
            } catch (e: IOException) {
                println(e)
                e.printStackTrace()
                motionString = e.message!!
            }
            if (motionString.isNotEmpty()) {
                println("motionReader will post " + motionString)
                val view = act.findViewById<View>(android.R.id.content)
                view.post {
                    view.findViewById<TextView>(R.id.motion).text = motionString
                    if (motionString.equals("0->1"))
                        act.wakeUpDevice()
                }
                println("fillFromUrl posted")
            }
        }
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
