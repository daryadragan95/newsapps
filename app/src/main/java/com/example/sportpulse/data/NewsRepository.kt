package com.example.sportpulse.data

import android.util.Xml
import com.example.sportpulse.domain.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

class NewsRepository {
    suspend fun latestNews(sport: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val feedUrl = feedUrlFor(sport)
        parseRss(getXml(feedUrl), source = "ESPN")
            .take(20)
    }

    private fun feedUrlFor(sport: String): String = when (sport) {
        "Basketball" -> "https://www.espn.com/espn/rss/nba/news"
        "Ice Hockey" -> "https://www.espn.com/espn/rss/nhl/news"
        "American Football" -> "https://www.espn.com/espn/rss/nfl/news"
        else -> "https://www.espn.com/espn/rss/soccer/news"
    }

    private fun getXml(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
        }

        return try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) {
                error("RSS returned HTTP ${connection.responseCode}: $body")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRss(xml: String, source: String): List<NewsArticle> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(xml.reader())
        }
        val articles = mutableListOf<NewsArticle>()
        var insideItem = false
        var tag: String? = null
        var title = ""
        var description = ""
        var link = ""
        var publishedAt = ""
        var imageUrl: String? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    tag = parser.name
                    if (tag == "item") {
                        insideItem = true
                        title = ""
                        description = ""
                        link = ""
                        publishedAt = ""
                        imageUrl = null
                    } else if (insideItem && (tag == "enclosure" || tag == "media:content")) {
                        imageUrl = parser.getAttributeValue(null, "url")
                    }
                }

                XmlPullParser.TEXT -> {
                    if (!insideItem) continue
                    val text = parser.text.orEmpty().trim()
                    if (text.isEmpty()) continue
                    when (tag) {
                        "title" -> title += text
                        "description" -> description += text
                        "link" -> link += text
                        "pubDate" -> publishedAt += text
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        insideItem = false
                        if (title.isNotBlank() && link.isNotBlank()) {
                            articles += NewsArticle(
                                title = title,
                                description = description.stripHtml(),
                                link = link,
                                publishedAt = publishedAt,
                                source = source,
                                imageUrl = imageUrl,
                            )
                        }
                    }
                    tag = null
                }
            }
        }
        return articles
    }

    private fun String.stripHtml(): String =
        replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()

    private companion object {
        const val TIMEOUT_MILLIS = 12_000
    }
}
