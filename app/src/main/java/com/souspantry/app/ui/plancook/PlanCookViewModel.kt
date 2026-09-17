package com.souspantry.app.ui.plancook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.RECIPE_REASON_PREFIX
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.MealHistoryRepository
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.data.repository.ShoppingRepository
import com.souspantry.app.services.ApiService
import com.souspantry.app.services.DirectClaude
import com.souspantry.app.services.RecipeLink
import com.souspantry.app.services.RecipeLinkResolver
import com.souspantry.app.services.RecipeImages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

// ── Plan & Cook page model ───────────────────────────────────────────────────

enum class PlanTab { DISCOVER, SAVED_RECIPES, MY_RECIPES, MY_PLANS }

enum class ChatRole { USER, ASSISTANT }

sealed interface ChatMessage {
    val id   : String
    val role : ChatRole

    data class Text(
        override val id   : String = UUID.randomUUID().toString(),
        override val role : ChatRole,
        val content       : String,
    ) : ChatMessage

    /** A list of clickable recipe cards, rendered directly with no bubble — mirrors iOS. */
    data class RecipeList(
        override val id   : String = UUID.randomUUID().toString(),
        override val role : ChatRole = ChatRole.ASSISTANT,
        val recipes       : List<SuggestedMeal>,
    ) : ChatMessage
}

data class PlanCookState(
    val selectedTab      : PlanTab             = PlanTab.DISCOVER,
    val messages         : List<ChatMessage>   = emptyList(),
    val inputText        : String              = "",
    val isLoading        : Boolean             = false,
    val selectedMoods    : Set<String>         = emptySet(),
    val selectedCuisines : Set<String>         = emptySet(),
    val pantryCount      : Int                 = 0,
    val pantryItems      : List<com.souspantry.app.data.models.PantryItem> = emptyList(),
    val userName         : String              = "",
    val error            : String?             = null,
    val isPremium        : Boolean             = false,
)

@HiltViewModel
class PlanCookViewModel @Inject constructor(
    private val api      : ApiService,
    private val claude   : DirectClaude,
    private val linkResolver : RecipeLinkResolver,
    private val repo     : PantryRepository,
    private val prefs    : UserPreferencesRepository,
    private val shopping : ShoppingRepository,
    private val history  : MealHistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanCookState())
    val state = _state.asStateFlow()

    val historySessions: StateFlow<List<MealHistorySession>> =
        history.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var generationJob: Job? = null
    private var moodAutoTriggerJob: Job? = null
    private var hasGreeted = false

    init {
        viewModelScope.launch {
            val name        = prefs.userName.first()
            val pantryItems = repo.items.first()
            _state.update { it.copy(userName = name, pantryCount = pantryItems.size) }
            sendGreeting()
        }
        // Premium = a paywall purchase OR the debug force toggle.
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(prefs.premiumActive, prefs.forcePremium) { active, force ->
                active || force
            }.collect { premium ->
                _state.update { it.copy(isPremium = premium) }
            }
        }
        // Keep the live pantry list in state for ingredient matching / week feed.
        viewModelScope.launch {
            repo.items.collect { items ->
                _state.update { it.copy(pantryItems = items, pantryCount = items.size) }
            }
        }
    }

    // ── Tab + filter writes ─────────────────────────────────────────────────

    fun selectTab(tab: PlanTab) = _state.update { it.copy(selectedTab = tab) }

    fun toggleMood(mood: String) {
        _state.update { s ->
            s.copy(
                selectedMoods = if (s.selectedMoods.contains(mood)) s.selectedMoods - mood
                                else                                 s.selectedMoods + mood
            )
        }
        scheduleMoodAutoTrigger()
    }

    /**
     * Mirrors iOS: picking a mood chip auto-fires a search after a short pause
     * so the user doesn't also have to tap send. Re-toggling within the pause
     * resets the debounce.
     */
    private fun scheduleMoodAutoTrigger() {
        moodAutoTriggerJob?.cancel()
        if (_state.value.selectedMoods.isEmpty()) return
        moodAutoTriggerJob = viewModelScope.launch {
            delay(1_500)
            val moods = _state.value.selectedMoods.sorted()
            if (moods.isEmpty()) return@launch
            sendMessage("I'm feeling ${moods.joinToString(", ")}")
            _state.update { it.copy(selectedMoods = emptySet(), selectedCuisines = emptySet()) }
        }
    }

    fun toggleCuisine(cuisine: String) = _state.update { s ->
        s.copy(
            selectedCuisines = if (s.selectedCuisines.contains(cuisine)) s.selectedCuisines - cuisine
                                else                                       s.selectedCuisines + cuisine
        )
    }

    fun setInputText(value: String) = _state.update { it.copy(inputText = value) }

    // ── Greeting + reset ────────────────────────────────────────────────────

    /**
     * Greeting text only — mirrors iOS, which waits for the user to actually
     * ask for something before showing any recipe cards. No auto-fetch here;
     * a starter list right under the greeting duplicated whatever the user's
     * first message produced next.
     */
    fun sendGreeting() {
        if (hasGreeted) return
        hasGreeted = true
        val first   = _state.value.userName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "there"
        val pantry  = _state.value.pantryCount
        val mealLbl = currentMealLabel()
        val greeting = buildString {
            append(timeGreeting()).append(" ").append(first).append("! ")
            if (pantry > 0) {
                append("You've got $pantry item${if (pantry == 1) "" else "s"} in your pantry. ")
            }
            append("What are you thinking for ").append(mealLbl).append("?")
        }
        appendAssistant(ChatMessage.Text(role = ChatRole.ASSISTANT, content = greeting))
    }

    fun startOver() {
        generationJob?.cancel()
        moodAutoTriggerJob?.cancel()
        hasGreeted = false
        _state.update {
            it.copy(
                messages         = emptyList(),
                inputText        = "",
                isLoading        = false,
                selectedMoods    = emptySet(),
                selectedCuisines = emptySet(),
                error            = null,
            )
        }
        sendGreeting()
    }

    // ── Send a user message + fetch assistant recipes ───────────────────────

    fun sendMessage(text: String = _state.value.inputText) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _state.value.isLoading) return

        // 1) Echo user bubble
        _state.update {
            it.copy(
                messages  = it.messages + ChatMessage.Text(role = ChatRole.USER, content = trimmed),
                inputText = "",
                isLoading = true,
                error     = null,
            )
        }

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val items    = repo.items.first()
            val moods    = _state.value.selectedMoods
            val cuisines = _state.value.selectedCuisines
            val dietCtx  = dietaryContext()

            runCatching {
                // Model calls go through the Worker proxy (keys stay server-side).
                // The legacy backend route is only a fallback if no app secret is set.
                if (claude.enabled) {
                    claude.chatMeals(items, userMessage = trimmed, moods = moods, cuisines = cuisines, dietaryContext = dietCtx)
                } else {
                    val body: MutableMap<String, Any> = mutableMapOf(
                        "pantryItems" to items.map { mapOf("name" to it.name, "category" to (it.category ?: "")) },
                        "userMessage" to trimmed,
                    )
                    if (dietCtx.isNotBlank())  body["dietaryContext"] = dietCtx
                    if (moods.isNotEmpty())    body["moods"]    = moods.toList()
                    if (cuisines.isNotEmpty()) body["cuisines"] = cuisines.toList()
                    api.chatMeals(body)
                }
            }
                .onSuccess { meals ->
                    val pick = stampPastedSource(meals.take(3), trimmed)
                    if (pick.isEmpty()) {
                        appendAssistant(ChatMessage.Text(
                            role = ChatRole.ASSISTANT,
                            content = "I couldn't find a match for that — try a different mood or cuisine?",
                        ))
                    } else {
                        appendAssistant(ChatMessage.RecipeList(recipes = pick))
                        saveHistorySession(pick)
                        // Start image generation now, so the photo exists by the time a recipe is saved.
                        pick.forEach { RecipeImages.warmUp(it.title, it.cuisine) }
                        // Scrape external pages now so "What to buy" shows the real ingredients instantly.
                        pick.forEach { meal -> viewModelScope.launch { scrapedIngredients(meal) } }
                    }
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure {
                    android.util.Log.e("PlanCookVM", "sendMessage generation failed", it)
                    appendAssistant(ChatMessage.Text(
                        role = ChatRole.ASSISTANT,
                        content = "I'm having trouble reaching the kitchen brain right now. Try again in a moment.",
                    ))
                    _state.update { it.copy(isLoading = false, error = "Network error") }
                }
        }
    }

    fun cancelSend() {
        generationJob?.cancel()
        _state.update { it.copy(isLoading = false) }
    }

    // ── Cooking session actions (from a Discover recipe) ─────────────────────

    /**
     * Marks a Discover recipe cooked: deducts the checked (in-pantry)
     * ingredients from the pantry by name match. Mirrors MyPlansViewModel's
     * markCooked, minus the week-entry removal since this meal was never
     * scheduled.
     */
    fun markCooked(checkedIngredients: List<String>) = viewModelScope.launch {
        val pantryItems = repo.items.first()
        checkedIngredients.forEach { ing ->
            val match = pantryItems.firstOrNull {
                it.name.contains(ing, ignoreCase = true) || ing.contains(it.name, ignoreCase = true)
            }
            if (match != null) repo.delete(match)
        }
    }

    /** Adds missing recipe ingredients to the shopping list (Essential, AI source). */
    fun addMissingToShopping(missing: List<String>, recipeTitle: String) = viewModelScope.launch {
        missing.forEach { name ->
            if (!shopping.exists(name)) {
                shopping.upsert(
                    ShoppingItem(
                        name     = name,
                        category = null,
                        quantity = null,
                        priority = "essential",
                        // Shown under the item in Shopping, so recipe items can be
                        // told apart from everything else on the list.
                        reason   = "$RECIPE_REASON_PREFIX$recipeTitle",
                    )
                )
            }
        }
    }

    /**
     * The AI "dietary context" sent with every generation request — dietary
     * types + restrictions + "avoid x, y…" + meal-mood phrases (mirrors iOS).
     */
    private suspend fun dietaryContext(): String {
        val types        = prefs.dietaryTypes.first().filterNot { it.isBlank() || it == "none" }
        val restrictions = prefs.foodRestrictions.first().filterNot { it.isBlank() || it == "none" }
        val avoid        = prefs.avoidIngredients.first().filter { it.isNotBlank() }
        val moodPhrases  = prefs.funnelMoods.first().mapNotNull { com.souspantry.app.ui.funnel.MOOD_PHRASES[it] }
        return buildString {
            if (types.isNotEmpty())        append("Dietary type: ${types.joinToString(", ")}. ")
            if (restrictions.isNotEmpty()) append("Restrictions: ${restrictions.joinToString(", ")}. ")
            if (avoid.isNotEmpty())        append("avoid ${avoid.joinToString(", ")}. ")
            if (moodPhrases.isNotEmpty())  append("Favour ${moodPhrases.joinToString("; ")}.")
        }.trim()
    }

    // ── External recipe links ───────────────────────────────────────────────

    /**
     * Resolve what a card's "View Recipe" should open. A card is external when
     * it carries a source, or when the user pasted a URL in this chat at all —
     * the paste is what we're honouring.
     */
    /**
     * When the user pasted a single recipe URL and the model didn't echo it back,
     * credit the source anyway — otherwise the card reads "Sous AI" for someone
     * else's recipe. Only fills blanks; an echoed URL is left as-is, and nothing
     * is stamped when the paste is ambiguous (several links, or several recipes).
     */
    private fun stampPastedSource(meals: List<SuggestedMeal>, userMessage: String): List<SuggestedMeal> {
        val pasted = RecipeLinkResolver.extractUrls(userMessage).distinct()
        if (pasted.size != 1 || meals.size != 1) return meals
        val url = pasted.first()
        return meals.map { meal ->
            if (meal.sourceURL.isNotBlank() || meal.sourceSite.isNotBlank()) meal
            else meal.copy(sourceURL = url, sourceSite = RecipeLinkResolver.host(url))
        }
    }

    /** Real ingredient lines from an external recipe's page; empty for Sous recipes or pages without JSON-LD. */
    suspend fun scrapedIngredients(meal: SuggestedMeal): List<String> =
        meal.sourceURL.takeIf { it.startsWith("http", ignoreCase = true) }
            ?.let { linkResolver.scrapedIngredients(it) }
            .orEmpty()

    fun isExternal(meal: SuggestedMeal): Boolean =
        meal.sourceURL.isNotBlank() || meal.sourceSite.isNotBlank() || pastedUrls().isNotEmpty()

    /** User messages only, newest first (never assistant messages). */
    private fun userTexts(): List<String> = _state.value.messages
        .filterIsInstance<ChatMessage.Text>()
        .filter { it.role == ChatRole.USER }
        .map { it.content }
        .reversed()

    private fun pastedUrls(): List<String> =
        userTexts().flatMap { RecipeLinkResolver.extractUrls(it) }

    fun resolveLink(meal: SuggestedMeal, onResolved: (RecipeLink) -> Unit) = viewModelScope.launch {
        val link = runCatching {
            linkResolver.resolve(
                cardUrl    = meal.sourceURL.ifBlank { null },
                cardDomain = meal.sourceSite.ifBlank { null },
                recipeName = meal.title,
                userTexts  = userTexts(),
            )
        }.getOrElse {
            android.util.Log.w("PlanCookVM", "link resolve failed", it)
            // Never strand the user: fall back to the pasted link if there is one.
            pastedUrls().firstOrNull()?.let { u -> RecipeLink.Direct(u) }
                ?: RecipeLink.Search(RecipeLinkResolver.googleSiteSearch(meal.sourceSite, meal.title))
        }
        onResolved(link)
    }

    // ── History ────────────────────────────────────────────────────────────

    private fun saveHistorySession(meals: List<SuggestedMeal>) = viewModelScope.launch {
        history.insert(
            MealHistorySession(
                cuisines = meals.map { it.cuisine }.filter { it.isNotBlank() }.distinct(),
                meals    = meals,
            )
        )
    }

    fun clearHistory() = viewModelScope.launch { history.clearAll() }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun appendAssistant(msg: ChatMessage) = _state.update { it.copy(messages = it.messages + msg) }

    private fun timeGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        else      -> "Good evening"
    }

    private fun currentMealLabel(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..10  -> "breakfast"
        in 11..14 -> "lunch"
        in 15..17 -> "an afternoon snack"
        else      -> "dinner"
    }
}
