package com.example.fatecskip

import com.example.fatecskip.data.repository.ExternalSoundRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MyInstantsRealParserTest {

    @Test
    fun `test parser with exact CMD output`() {
        val html = """<button class="small-button" onclick="play('/media/sounds/a-risada-do-kiko.mp3', 'loader-313473', 'a-risada-do-kiko-65965')" title="Tocar o som de A risada do kiko" type="button"></button>"""
        val list = ExternalSoundRepository.parseHtml(html)

        assertTrue("A lista não deveria estar vazia!", list.isNotEmpty())
        val item = list.first()
        println("Test 1 Result -> ID: ${item.id} | Title: ${item.title} | MP3: ${item.mp3}")

        assertEquals("a-risada-do-kiko-65965", item.id)
        assertEquals("A risada do kiko", item.title)
        assertEquals("https://www.myinstants.com/media/sounds/a-risada-do-kiko.mp3", item.mp3)
    }

    @Test
    fun `test unicode escapes and safe filter`() {
        val html = """<button class="small-button" onclick="play('/media/sounds/jogo\u002Ddo-bot\u00e3o.mp3', 'loader-1', 'jogo\u002Ddo-bot\u00e3o')" title="Tocar o som de Jogo do bot\u00e3o" type="button"></button><button class="small-button" onclick="play('/media/sounds/gemidao.mp3', 'loader-2', 'gemidao')" title="Tocar o som de Gemidão" type="button"></button>"""
        val list = ExternalSoundRepository.parseHtml(html)

        assertEquals(1, list.size)
        assertEquals("Jogo do botão", list.first().title)
        assertEquals(
            "https://www.myinstants.com/media/sounds/jogo-do-botão.mp3",
            list.first().mp3
        )
    }

    @Test
    fun `test blocked keywords in title id and mp3 are filtered`() {
        val html = """<button class="small-button" onclick="play('/media/sounds/good-morning.mp3', 'loader-1', 'safe-id')" title="Tocar o som de Bom dia" type="button"></button><button class="small-button" onclick="play('/media/sounds/gemidao.mp3', 'loader-2', 'unsafe-id')" title="Tocar o som de Som qualquer" type="button"></button><button class="small-button" onclick="play('/media/sounds/safe-sound.mp3', 'loader-3', 'fdr-meme')" title="Tocar o som de Som qualquer" type="button"></button><button class="small-button" onclick="play('/media/sounds/safe-sound.mp3', 'loader-4', 'safe-id-2')" title="Tocar o som de Porra qualquer" type="button"></button><button class="small-button" onclick="play('/media/sounds/estourado.mp3', 'loader-5', 'safe-id-3')" title="Tocar o som de Som qualquer" type="button"></button>"""
        val list = ExternalSoundRepository.parseHtml(html)

        assertEquals(1, list.size)
        assertEquals("Bom dia", list.first().title)
    }

    @Test
    fun `test live fetch from myinstants BR`() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url("https://www.myinstants.com/pt/index/br/").build()
        val response = client.newCall(request).execute()
        val html = response.use {
            it.body()?.bytes()?.toString(Charsets.UTF_8).orEmpty()
        }
        assertTrue("HTML body should not be empty", html.isNotEmpty())

        val list = ExternalSoundRepository.parseHtml(html)

        println("=== Live Fetch Results ===")
        println("Total items found: ${list.size}")
        list.take(5).forEach { item ->
            println("- ID: ${item.id} | Title: ${item.title} | MP3: ${item.mp3}")
        }
        println("==========================")

        assertTrue("Live fetch deve extrair sons!", list.isNotEmpty())
        assertTrue(
            "O título Jogo do botão deve preservar o caractere ã",
            list.any { it.title == "Jogo do botão" }
        )
    }
}
