package com.example.fatecskip.data.repository

import android.content.Context
import android.util.Log
import com.example.fatecskip.data.remote.MyInstantsApiClient
import com.example.fatecskip.data.remote.dto.MyInstantSoundDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.regex.Pattern

class ExternalSoundRepository(private val context: Context) {
    private val htmlClient = MyInstantsApiClient.client

    suspend fun getTrendingSoundsBr(): List<MyInstantSoundDto> = withContext(Dispatchers.IO) {
        fetchHtmlAndParse("https://www.myinstants.com/pt/index/br/")
    }

    suspend fun searchSoundsPtBr(query: String): List<MyInstantSoundDto> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            fetchHtmlAndParse("https://www.myinstants.com/pt/search/?name=$encodedQuery")
        } catch (e: Exception) {
            Log.e("ExternalSound", "Erro ao encodar query", e)
            emptyList()
        }
    }

    private fun fetchHtmlAndParse(targetUrl: String): List<MyInstantSoundDto> {
        return try {
            Log.d("ExternalSound", "Buscando HTML direto: $targetUrl")
            val request = Request.Builder().url(targetUrl).build()
            htmlClient.newCall(request).execute().use { response ->
                Log.d("ExternalSound", "Código HTTP MyInstants: ${response.code()}")
                if (!response.isSuccessful) return emptyList()

                val html = response.body()?.bytes()?.toString(Charsets.UTF_8) ?: return emptyList()
                val list = parseHtml(html)
                Log.d("ExternalSound", "Extraídos ${list.size} sons seguros do HTML")
                list
            }
        } catch (e: Exception) {
            Log.e("ExternalSound", "Erro ao fazer o parse do HTML direto", e)
            emptyList()
        }
    }

    companion object {
        private val blockedKeywords = listOf(
            "gemid",
            "fdr",
            "fdp",
            "cracudo",
            "porr",
            "caralh",
            "bucet",
            "puta",
            "pinto",
            "piroca",
            "fod",
            "porn",
            "sexo",
            "nude",
            "putaria",
            "estourad",
            "orgasm",
            "hentai"
        )

        private val unicodeEscapeRegex = Regex("""\\u([0-9a-fA-F]{4})""")
        private val playRegex = Pattern.compile(
            "onclick=\"play\\('(/media/sounds/[^']+\\.mp3)',\\s*'[^']+',\\s*'([^']+)'\\)\"\\s+title=\"Tocar o som de ([^\"]+)\""
        )
        private val shareRegex = Pattern.compile(
            "onclick=\"share\\('([^']+)',\\s*'[^']+',\\s*'(/media/sounds/[^']+\\.mp3)',\\s*'([^']+)'\\)\""
        )

        internal fun parseHtml(html: String): List<MyInstantSoundDto> {
            val list = mutableListOf<MyInstantSoundDto>()
            val seenIds = mutableSetOf<String>()
            val playMatcher = playRegex.matcher(html)

            while (playMatcher.find()) {
                val mp3Path = playMatcher.group(1) ?: continue
                val id = playMatcher.group(2) ?: continue
                val rawTitle = playMatcher.group(3) ?: continue
                addIfSafe(list, seenIds, id, rawTitle, mp3Path)
            }

            if (list.isEmpty()) {
                val shareMatcher = shareRegex.matcher(html)
                while (shareMatcher.find()) {
                    val rawTitle = shareMatcher.group(1) ?: continue
                    val mp3Path = shareMatcher.group(2) ?: continue
                    val id = shareMatcher.group(3) ?: continue
                    addIfSafe(list, seenIds, id, rawTitle, mp3Path)
                }
            }

            return list.filterNot { sound ->
                val searchableText = "${sound.title} ${sound.id} ${sound.mp3}".lowercase()
                blockedKeywords.any(searchableText::contains)
            }
        }

        private fun addIfSafe(
            list: MutableList<MyInstantSoundDto>,
            seenIds: MutableSet<String>,
            rawId: String,
            rawTitle: String,
            rawMp3Path: String
        ) {
            val id = cleanHtmlText(rawId)
            val title = cleanHtmlText(rawTitle)
            val mp3Path = cleanHtmlText(rawMp3Path)
            if (seenIds.add(id)) {
                list.add(
                    MyInstantSoundDto(
                        id = id,
                        title = title,
                        mp3 = "https://www.myinstants.com$mp3Path"
                    )
                )
            }
        }

        private fun cleanHtmlText(raw: String): String {
            val decodedUnicode = unicodeEscapeRegex.replace(raw) { match ->
                match.groupValues[1].toInt(16).toChar().toString()
            }
            return decodedUnicode
                .replace("&#x27;", "'")
                .replace("&#39;", "'")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("\n", "")
                .trim()
        }
    }

    suspend fun downloadAndCacheSound(soundId: String, mp3Url: String): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "custom_alarms")
            if (!dir.exists()) dir.mkdirs()

            dir.listFiles()?.forEach { it.delete() }

            val file = File(dir, "$soundId.mp3")
            
            val request = Request.Builder().url(mp3Url).build()
            val response = htmlClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.peekBody(Long.MAX_VALUE)
                responseBody.byteStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ExternalSound", "Erro ao baixar MP3", e)
            null
        }
    }
}
