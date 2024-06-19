package com.dev114.pingcommand

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {


    private lateinit var pingButton: Button
    private lateinit var stopButton: Button
    private lateinit var clearButton: Button
    private lateinit var pasteButton: Button
    private lateinit var copyButton: Button
    private lateinit var infoButton: Button
    private lateinit var ipAddressEditText: EditText
    private lateinit var resultTextView: TextView
    private lateinit var detailedResultTextView: TextView
    private var pingTask: PingTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pingButton = findViewById(R.id.pingButton)
        stopButton = findViewById(R.id.stopButton)
        clearButton = findViewById(R.id.clearButton)
        pasteButton = findViewById(R.id.pasteButton)
        copyButton = findViewById(R.id.copyButton)
        infoButton = findViewById(R.id.infoButton)
        ipAddressEditText = findViewById(R.id.ipAddressEditText)
        resultTextView = findViewById(R.id.resultTextView)
        detailedResultTextView = findViewById(R.id.detailedResultTextView)



        pingButton.setOnClickListener {
            startPing()
        }

        stopButton.setOnClickListener {
            stopPing()
        }

        clearButton.setOnClickListener {
            clearText()
        }

        pasteButton.setOnClickListener {
            pasteText()
        }

        copyButton.setOnClickListener {
            copyText()
        }


        infoButton.setOnClickListener {
            // Creating an Intent to move from MainActivity to MainActivity2
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }


    }

    private fun startPing() {
        val ipAddress = ipAddressEditText.text.toString().trim()
        if (ipAddress.isNotEmpty()) {
            if (pingTask == null || pingTask?.status != AsyncTask.Status.RUNNING) {
                pingTask = PingTask()
                pingTask?.execute(ipAddress)
            }
        } else {
            resultTextView.text = "Please enter IP address or domain"
        }
    }

    private fun stopPing() {
        pingTask?.cancel(true)
    }

    private fun clearText() {
        ipAddressEditText.text.clear()
        resultTextView.text = ""
        detailedResultTextView.text = ""
    }

    private fun pasteText() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pasteData = clip.getItemAt(0).text.toString()
            ipAddressEditText.setText(pasteData)
        } else {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyText() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Ping Result", resultTextView.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Result copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    inner class PingTask : AsyncTask<String, String, Void>() {
        private var totalResponseTime: Long = 0
        private var minResponseTime: Long = Long.MAX_VALUE
        private var maxResponseTime: Long = Long.MIN_VALUE
        private var totalPacketsSent: Int = 0
        private var totalPacketsReceived: Int = 0

        override fun doInBackground(vararg params: String?): Void? {
            val host = params[0]
            val runtime = Runtime.getRuntime()
            try {
                val ipAddress = InetAddress.getByName(host)
                if (!ipAddress.isReachable(1500)) {
                    // Show toast if address is not reachable
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Address is not reachable", Toast.LENGTH_SHORT).show()
                    }
                    return null
                }
                while (!isCancelled) {
                    val startTime = System.currentTimeMillis()
                    val process = runtime.exec("/system/bin/ping -c 1 $host")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val detailedResult = reader.readText()
                    val reachable = ipAddress.isReachable(1500)
                    val endTime = System.currentTimeMillis()
                    val responseTime = endTime - startTime
                    totalResponseTime += responseTime
                    minResponseTime = kotlin.math.min(minResponseTime, responseTime)
                    maxResponseTime = kotlin.math.max(maxResponseTime, responseTime)
                    totalPacketsSent++
                    if (reachable) {
                        totalPacketsReceived++
                    }
                    val packetLossPercentage =
                        ((totalPacketsSent - totalPacketsReceived).toDouble() / totalPacketsSent.toDouble()) * 100
                    val liveResult =
                        "Packets Sent: $totalPacketsSent, Packets Received: $totalPacketsReceived, Packets Loss: ${
                            packetLossPercentage.roundToInt()
                        }%, Avg: ${totalResponseTime / totalPacketsReceived}ms, Min: $minResponseTime ms, Max: $maxResponseTime ms"
                    publishProgress(liveResult, detailedResult)
                    Thread.sleep(1000)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onProgressUpdate(vararg values: String?) {
            super.onProgressUpdate(*values)
            resultTextView.text = values[0]
            detailedResultTextView.text = values[1]
        }
    }
}