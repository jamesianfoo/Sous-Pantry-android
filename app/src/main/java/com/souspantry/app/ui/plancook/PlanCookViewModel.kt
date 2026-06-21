package com.souspantry.app.ui.plancook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    /** Assistant intro line + a list of clickable recipe cards. */
    data class RecipeList(
        override val id   : String = UUID.randomUUID().toString(),
        override val role : ChatRole = ChatRole.ASSISTANT,
        val intro         : String,
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
    private val api   : ApiService,
    private val repo  : PantryRepository,
    private val prefs : UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanCookState())
    val state = _state.asStateFlow()

    private var generationJob: Job? = null
    private var hasGreeted = false

    init {
        viewModelScope.launch {
            val name        = prefs.userName.first()
            val pantryItems = repo.items.first()
            _state.update { it.copy(userName = name, pantryCount = pantryItems.size) }
            sendGreeting()
        }
        // Live-update the premium flag whenever the debug toggle flips.
        viewModelScope.launch {
            prefs.forcePremium.collect { force ->
                _state.update { it.copy(isPremium = force) }
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

    fun toggleMood(mood: String) = _state.update { s ->
        s.copy(
            selectedMoods = if (s.selectedMoods.contains(mood)) s.selectedMoods - mood
                            else                                 s.selectedMoods + mood
        )
    }

    fun toggleCuisine(cuisine: String) = _state.update { s ->
        s.copy(
            selectedCuisines = if (s.selectedCuisines.contains(cuisine)) s.selectedCuisines - cuisine
                                else                                       s.selectedCuisines + cuisine
        )
    }

    fun setInputText(value: String) = _state.update { it.copy(inputText = value) }

    // ── Greeting + reset ────────────────────────────────────────────────────

    fun sendGreeting() {
        if (hasGreeted) return
        hasGreeted = true
        viewModelScope.launch {
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

            // Then fetch 3 starter recipes from the backend
            runCatching { api.fetchSuggestedMeals(emptyMap()) }
                .onSuccess { meals ->
                    val pick = meals.take(3)
                    if (pick.isNotEmpty()) {
                        appendAssistant(
                            ChatMessage.RecipeList(
                                intro   = "Here's ${pick.size} thing${if (pick.size == 1) "" else "s"} you can make right now with what's in your pantry:",
                                recipes = pick,
                            )
                        )
                    }
                }
                // Silent on failure — keep greeting visible, just don't show cards
        }
    }

    fun startOver() {
        generationJob?.cancel()
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
            val items = repo.items.first()
            val body: MutableMap<String, Any> = mutableMapOf(
                "pantryItems" to items.map { mapOf("name" to it.name, "category" to (it.category ?: "")) },
                "userMessage" to trimmed,
            )
            val moods    = _state.value.selectedMoods
            val cuisines = _state.value.selectedCuisines
            if (moods.isNotEmpty())    body["moods"]    = moods.toList()
            if (cuisines.isNotEmpty()) body["cuisines"] = cuisines.toList()

            runCatching { api.generateMeals(body) }
                .onSuccess { meals ->
                    val pick = meals.take(3)
                    if (pick.isEmpty()) {
                        appendAssistant(ChatMessage.Text(
                            role = ChatRole.ASSISTANT,
                            content = "I couldn't find a match for that — try a different mood or cuisine?",
                        ))
                    } else {
                        appendAssistant(ChatMessage.RecipeList(
                            intro   = "Here's what I'd suggest:",
                            recipes = pick,
                        ))
                    }
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure {
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
