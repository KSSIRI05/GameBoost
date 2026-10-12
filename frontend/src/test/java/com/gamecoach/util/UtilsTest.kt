package com.gamecoach.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.util.Locale

class UtilsTest {
    private lateinit var originalLocale: Locale

    @Before
    fun useFrenchLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.FRANCE)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    // ─────────────────────────────────────────────
    // formatDate
    // ─────────────────────────────────────────────

    @Test
    fun `formatDate retourne la date originale si le format est invalide`() {
        val invalide = "pas-une-date"
        val result = Utils.formatDate(invalide)
        assertEquals(invalide, result)
    }

    @Test
    fun `formatDate retourne la date originale si la chaine est vide`() {
        val result = Utils.formatDate("")
        assertEquals("", result)
    }

    // ─────────────────────────────────────────────
    // formatCurrency
    // ─────────────────────────────────────────────

    @Test
    fun `formatCurrency formate un montant entier avec deux decimales`() {
        val result = Utils.formatCurrency(100.0)
        assertEquals("100,00€", result)
    }

    @Test
    fun `formatCurrency formate un montant decimal`() {
        val result = Utils.formatCurrency(35.5)
        assertEquals("35,50€", result)
    }

    @Test
    fun `formatCurrency formate un montant zero`() {
        val result = Utils.formatCurrency(0.0)
        assertEquals("0,00€", result)
    }

    @Test
    fun `formatCurrency formate un grand montant`() {
        val result = Utils.formatCurrency(1999.99)
        assertEquals("1999,99€", result)
    }

    // ─────────────────────────────────────────────
    // getRanksByGame
    // ─────────────────────────────────────────────

    @Test
    fun `getRanksByGame retourne les rangs Valorant non vides`() {
        val ranks = Utils.getRanksByGame("Valorant")
        assertTrue("Les rangs Valorant ne doivent pas être vides", ranks.isNotEmpty())
    }

    @Test
    fun `getRanksByGame inclut Iron dans les rangs Valorant`() {
        val ranks = Utils.getRanksByGame("Valorant")
        assertTrue(ranks.contains("Iron"))
    }

    @Test
    fun `getRanksByGame inclut Radiant dans les rangs Valorant`() {
        val ranks = Utils.getRanksByGame("Valorant")
        assertTrue(ranks.contains("Radiant"))
    }

    @Test
    fun `getRanksByGame retourne les rangs League of Legends non vides`() {
        val ranks = Utils.getRanksByGame("League of Legends")
        assertTrue(ranks.isNotEmpty())
    }

    @Test
    fun `getRanksByGame inclut Challenger dans les rangs LoL`() {
        val ranks = Utils.getRanksByGame("League of Legends")
        assertTrue(ranks.contains("Challenger"))
    }

    @Test
    fun `getRanksByGame retourne les rangs CS2 non vides`() {
        val ranks = Utils.getRanksByGame("Counter-Strike 2")
        assertTrue(ranks.isNotEmpty())
    }

    @Test
    fun `getRanksByGame inclut Global Elite dans les rangs CS2`() {
        val ranks = Utils.getRanksByGame("Counter-Strike 2")
        assertTrue(ranks.contains("Global Elite"))
    }

    @Test
    fun `getRanksByGame retourne les rangs Overwatch 2 non vides`() {
        val ranks = Utils.getRanksByGame("Overwatch 2")
        assertTrue(ranks.isNotEmpty())
    }

    @Test
    fun `getRanksByGame retourne une liste vide pour un jeu inconnu`() {
        val ranks = Utils.getRanksByGame("JeuInexistant")
        assertTrue("Un jeu inconnu doit retourner une liste vide", ranks.isEmpty())
    }

    @Test
    fun `getRanksByGame retourne une liste vide pour une chaine vide`() {
        val ranks = Utils.getRanksByGame("")
        assertTrue(ranks.isEmpty())
    }

    // ─────────────────────────────────────────────
    // getCurrentDate / getCurrentTime (non null, non vide)
    // ─────────────────────────────────────────────

    @Test
    fun `getCurrentDate retourne une chaine non vide`() {
        val date = Utils.getCurrentDate()
        assertTrue(date.isNotBlank())
    }

    @Test
    fun `getCurrentDate respecte le format yyyy-MM-dd`() {
        val date = Utils.getCurrentDate()
        val regex = Regex("""\d{4}-\d{2}-\d{2}""")
        assertTrue("Format attendu : yyyy-MM-dd, obtenu : $date", regex.matches(date))
    }

    @Test
    fun `getCurrentTime retourne une chaine non vide`() {
        val time = Utils.getCurrentTime()
        assertTrue(time.isNotBlank())
    }

    @Test
    fun `getCurrentTime respecte le format HH-mm`() {
        val time = Utils.getCurrentTime()
        val regex = Regex("""\d{2}:\d{2}""")
        assertTrue("Format attendu : HH:mm, obtenu : $time", regex.matches(time))
    }
}
