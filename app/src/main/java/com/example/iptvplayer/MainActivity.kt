package com.example.iptvplayer

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var allChannels = listOf<Channel>()
    private var filteredChannels = listOf<Channel>()
    private lateinit var lvChannels: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etUrl = findViewById<EditText>(R.id.etM3uUrl)
        val btnLoad = findViewById<Button>(R.id.btnLoad)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        lvChannels = findViewById(R.id.lvChannels)
        playerView = findViewById(R.id.playerView)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Load previously saved URL
        val prefs = getSharedPreferences("IPTVPrefs", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("last_url", "")
        if (!savedUrl.isNullOrEmpty()) {
            etUrl.setText(savedUrl)
            loadPlaylist(savedUrl)
        }

        btnLoad.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                prefs.edit().putString("last_url", url).apply()
                loadPlaylist(url)
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                filteredChannels = allChannels.filter { it.name.lowercase().contains(query) }
                updateListView()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        lvChannels.setOnItemClickListener { _, _, position, _ ->
            playChannel(filteredChannels[position].url)
        }
    }

    private fun loadPlaylist(urlString: String) {
        Toast.makeText(this, "Loading playlist...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val content = URL(urlString).readText()
                allChannels = M3uParser.parse(content)
                filteredChannels = allChannels
                withContext(Dispatchers.Main) {
                    updateListView()
                    Toast.makeText(this@MainActivity, "${allChannels.size} channels loaded", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateListView() {
        val names = filteredChannels.map { it.name }
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.WHITE)
                return view
            }
        }
        lvChannels.adapter = adapter
    }

    private fun playChannel(streamUrl: String) {
        val mediaItem = MediaItem.fromUri(streamUrl)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
