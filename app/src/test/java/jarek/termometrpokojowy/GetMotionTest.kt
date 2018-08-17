package jarek.termometrpokojowy

import org.junit.Test

import org.junit.Assert.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class GetMotionTest {
    @Test
    fun test() {
        val url = URL("https://eventserver75.herokuapp.com/events/read?code=weather")
        println("get connection")
        val conn = url.openConnection() as HttpURLConnection
        println("connection response: " + conn.responseCode)
        val istr = conn.getInputStream()!!
        println("stream $istr")
    }
}
