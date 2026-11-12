package com.example.fatecskip

import com.example.fatecskip.data.repository.ExternalSoundRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `test sample MyInstants HTML parses UTF-8 and filters blocked terms offline`() {
        val html = """
            <button class="small-button" onclick="play('/media/sounds/jogo-do-botão.mp3', 'loader-1', 'jogo-do-botão-65965')" title="Tocar o som de Jogo do botão" type="button"></button>
            <button class="small-button" onclick="play('/media/sounds/gemidão.mp3', 'loader-2', 'safe-id')" title="Tocar o som de Som impróprio" type="button"></button>
            <button class="small-button" onclick="play('/media/sounds/som-seguro.mp3', 'loader-3', 'fdr-meme')" title="Tocar o som de Outro som" type="button"></button>
            <button class="small-button" onclick="play('/media/sounds/som-seguro.mp3', 'loader-4', 'safe-id-2')" title="Tocar o som de Porra qualquer" type="button"></button>
        """.trimIndent()
        val list = ExternalSoundRepository.parseHtml(html)

        assertEquals(1, list.size)
        assertEquals("jogo-do-botão-65965", list.single().id)
        assertEquals("Jogo do botão", list.single().title)
        assertEquals(
            "https://www.myinstants.com/media/sounds/jogo-do-botão.mp3",
            list.single().mp3
        )
    }
}
