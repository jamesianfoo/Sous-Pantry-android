package com.souspantry.app.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure matching helpers behind pasted-URL resolution. `extractUrls`
 * is excluded — it needs android.util.Patterns, which isn't on the JVM path.
 */
class RecipeLinkResolverTest {

    @Test fun `normalize ignores scheme, www, query and trailing slash`() {
        val a = RecipeLinkResolver.normalize("https://www.recipetineats.com/crispy-chilli-beef/")
        val b = RecipeLinkResolver.normalize("http://recipetineats.com/crispy-chilli-beef?utm_source=x")
        assertEquals(a, b)
        assertEquals("recipetineats.com/crispy-chilli-beef", a)
    }

    @Test fun `normalize keeps different pages distinct`() {
        assertFalse(
            RecipeLinkResolver.normalize("https://recipetineats.com/a") ==
                RecipeLinkResolver.normalize("https://recipetineats.com/b")
        )
    }

    @Test fun `domain match tolerates subdomains in both directions`() {
        assertTrue(RecipeLinkResolver.hostMatchesDomain("blog.recipetineats.com", "recipetineats.com"))
        assertTrue(RecipeLinkResolver.hostMatchesDomain("recipetineats.com", "www.recipetineats.com"))
        assertFalse(RecipeLinkResolver.hostMatchesDomain("nytimes.com", "recipetineats.com"))
        assertFalse(RecipeLinkResolver.hostMatchesDomain("", "recipetineats.com"))
    }

    @Test fun `title match tolerates site suffixes but rejects a different dish`() {
        assertTrue(RecipeLinkResolver.titleMatches("Crispy Chilli Beef", "Crispy Chilli Beef Recipe | RecipeTin Eats"))
        assertFalse(RecipeLinkResolver.titleMatches("Crispy Chilli Beef", "Chocolate Cake | Some Blog"))
    }

    @Test fun `host strips www and survives junk`() {
        assertEquals("recipetineats.com", RecipeLinkResolver.host("https://www.recipetineats.com/x"))
        assertEquals("", RecipeLinkResolver.host("not a url"))
    }

    @Test fun `search falls back to the bare recipe name without a domain`() {
        assertTrue(RecipeLinkResolver.googleSiteSearch("recipetineats.com", "Chilli Beef").contains("site%3A"))
        assertFalse(RecipeLinkResolver.googleSiteSearch("", "Chilli Beef").contains("site%3A"))
    }
}
