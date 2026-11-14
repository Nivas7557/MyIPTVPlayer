package com.example.iptvplayer

data class Channel(val name: String, val url: String)

object M3uParser {
    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentName = "Unknown Channel"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                val commaIndex = trimmed.lastIndexOf(",")
                if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    currentName = trimmed.substring(commaIndex + 1).trim()
                }
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                channels.add(Channel(currentName, trimmed))
                currentName = "Unknown Channel"
            }
        }
        return channels
    }
}
