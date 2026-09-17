package com.souspantry.app.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: android-pantry-staples-prompt.md — exact match only, never substring. */
class IngredientStaplesTest {

    @Test fun `staples match through leading and trailing qualifiers`() {
        listOf(
            "salt to taste",
            "Salt and pepper, to taste",
            "a pinch of salt",
            "a pinch of black pepper",
            "freshly ground black pepper",
            "oil for frying",
            "sugar",
            "ice",
        ).forEach { assertTrue(it, IngredientStaples.isPantryStaple(it)) }
    }

    @Test fun `staples match on full recipe lines with quantities`() {
        assertTrue(IngredientStaples.isPantryStaple("500ml boiling water"))
        assertTrue(IngredientStaples.isPantryStaple("2 tbsp vegetable oil"))
    }

    @Test fun `real ingredients that merely contain a staple word do not match`() {
        listOf(
            "salted butter",
            "100g salted butter",
            "sugar snap peas",
            "water chestnuts",
            "olive oil",
            "2 tbsp olive oil",
            "soy sauce",
            "plain flour",
            "chicken stock",
            "butter",
        ).forEach { assertFalse(it, IngredientStaples.isPantryStaple(it)) }
    }

    @Test fun `a recipe is fully matched when only staples are absent from the pantry`() {
        val ingredients = listOf(
            "salt to taste", "a pinch of black pepper", "500ml boiling water",
            "beef mince", "garlic", "rice",
        )
        val pantry = setOf("beef mince", "garlic", "rice")
        val missing = ingredients.filter { !IngredientStaples.isPantryStaple(it) && it !in pantry }
        assertTrue(missing.isEmpty())
    }
}
