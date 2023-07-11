package io.rownd.android.automations

import android.util.Log
import io.rownd.android.Rownd
import io.rownd.android.models.domain.*
import io.rownd.android.models.repos.GlobalState
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.util.RowndContext
import io.rownd.android.util.stringToSeconds
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

internal fun computeLastRunId(automation: Automation): String {
    val lastRunId = "automation_${automation.id}_last_run"
    Log.i("Rownd","Last run id: $lastRunId")
    return lastRunId
}

internal fun computeLastRunTimestamp(automation: Automation, meta: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>): Date? {
    val lastRunId = computeLastRunId(automation)
    val lastRunDate = meta[lastRunId] as String? ?: return null

    val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
    iso8601Format.timeZone = TimeZone.getTimeZone("UTC")

    return iso8601Format.parse(lastRunDate)
}

internal class AutomationsCoordinator(private val rowndContext: RowndContext) {
    private var automationJob: Job? = null
    init {
        CoroutineScope(Dispatchers.IO).launch {
            Rownd.store.stateAsStateFlow().collect() {
                automationJob?.cancel()
                automationJob = CoroutineScope(Dispatchers.IO).launch {
                    delay(2000)
                    processAutomations(it)
                }
            }
        }
    }

    private fun processAutomations(state: GlobalState) {
        val automations = state.appConfig.config.automations ?: run {
            return
        }

        for (automation in automations) {
            processAutomation(state, automation)
        }

    }

    private fun processAutomation(state: GlobalState, automation: Automation) {
        if (automation.state == AutomationState.disabled) {
            Log.i("Rownd","Automation is disabled: ${automation.name}")
            return
        }

        val willAutomationRun = shouldAutomationRun(state, automation)

        if (!willAutomationRun) {
            Log.i("Rownd","Automation does not need to run: ${automation.name}")
            return
        }

        automation.actions?.forEach { it
            invokeAction(type = it.type, args = it.args, automation = automation)
        }

        Log.i("Rownd","Run automation ${automation.name}")
    }

    private fun invokeAction(type: AutomationActionType?, args: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>?, automation: Automation) {
        val actionFn = AutomationActors.find { it.first == type }

        actionFn?.second?.invoke( args ) ?: return

        val lastRunId = computeLastRunId(automation)

        val currentTime = rowndContext.kronosClock?.getCurrentTimeMs() ?: System.currentTimeMillis()
        val currentDate = Date(currentTime)
        val instant = currentDate.toInstant()
        val iso8601String = DateTimeFormatter.ISO_INSTANT.format(instant)

        Rownd.userRepo.setMetaData(lastRunId, iso8601String)
    }

    private fun determineAutomationMetaData(state: GlobalState): Map<String, @Serializable(with = AnyValueSerializer::class) Any?> {
        var automationMeta = state.user.meta

        val hasPromptedForPasskey = automationMeta["last_passkey_registration_prompt"] != null
        val hasPasskeys = state.passkeys.registrations.isNotEmpty()

        val additionalAutomationMeta: Map<String, @Serializable(with = AnyValueSerializer::class) Any?> = mapOf(
            "is_authenticated" to state.auth.isAccessTokenValid,
            "is_verified" to state.auth.isVerifiedUser,
            "are_passkeys_initialized" to state.passkeys.isInitialized,
            "has_prompted_for_passkey" to hasPromptedForPasskey,
            "has_passkeys" to hasPasskeys
        )

        automationMeta = automationMeta + additionalAutomationMeta

        Log.i("Rownd","Meta data: $automationMeta")

        return automationMeta
    }

    private fun shouldAutomationRun(state: GlobalState, automation: Automation): Boolean {
        val automationMetaData = determineAutomationMetaData(state)

        val ruleResult = automation.rules?.all { rule ->
            val userData =
                if (rule.entityType == AutomationRuleEntityType.MetaData) automationMetaData else state.user.data

            evaluateRule(userData, rule)
        } ?: false

        if (!ruleResult) {
            return false
        }

        automation.triggers?.find { it.type == AutomationTriggerType.Time }?.let {
            it
            val lastRunDate = computeLastRunTimestamp(automation, meta = state.user.meta)

            return shouldTrigger(trigger = it, lastRunDate = lastRunDate)
        }

        return false
    }

    private fun shouldTrigger(trigger: AutomationTrigger, lastRunDate: Date?): Boolean {
        when (trigger.type) {
            AutomationTriggerType.Time -> {
                val lastRunDate = lastRunDate ?: return true
                val triggerFrequency = stringToSeconds(trigger.value) ?: return false

                val calendar = Calendar.getInstance()
                calendar.time = lastRunDate
                calendar.add(Calendar.SECOND, triggerFrequency)
                val dateOfNextPrompt = calendar.time

                val currentTime = rowndContext.kronosClock?.getCurrentTimeMs() ?: System.currentTimeMillis()
                val currentDate = Date(currentTime)

                return dateOfNextPrompt.before(currentDate)
            }
            else -> {
                return false
            }
        }
    }
}