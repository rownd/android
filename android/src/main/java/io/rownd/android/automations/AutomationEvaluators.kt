package io.rownd.android.automations

import android.util.Log
import io.rownd.android.models.domain.*
import io.rownd.android.util.AnyValueSerializer
import kotlinx.serialization.Serializable

internal fun evaluateRule(userData: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, rule: AutomationRule): Boolean {
    val userDataValue = userData[rule.attribute] ?: run {
        Log.i("Rownd","Attribute not found: ${rule.attribute}")
        return false
    }

    val conditionEvalFun = conditionEvaluators.find { it.first == rule.condition }

    val result = conditionEvalFun?.second?.invoke(userData, rule.attribute, rule.value ) ?: false

    Log.i("Rownd", "Rule (Condition, Attribute, attribute value, rule value, and result) ${rule.condition} ${rule.attribute} $userDataValue ${rule.value} $result")

    return result
}

internal fun conditionEvaluatorsEquals(data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, attribute: String, value: @Serializable(with = AnyValueSerializer::class) Any?): Boolean {
    Log.i("Rownd", "Condition: EQUALS")
    val dataValue = data[attribute] ?: run {
        return false
    }
    val attributeValue = value ?: run {
        return false
    }

    return dataValue.toString() == attributeValue.toString()
}

internal fun conditionEvaluatorsNotEquals(data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, attribute: String, value: @Serializable(with = AnyValueSerializer::class) Any?): Boolean {
    Log.i("Rownd", "Condition: NOT_EQUALS")

    val dataValue = data[attribute] ?: run {
        return false
    }
    val attributeValue = value ?: run {
        return false
    }

    return dataValue.toString() != attributeValue.toString()
}

internal fun conditionEvaluatorsContains(data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, attribute: String, value: @Serializable(with = AnyValueSerializer::class) Any?): Boolean {
    Log.i("Rownd", "Condition: CONTAINS")
    val dataValue = data[attribute] ?: run {
        return false
    }
    val attributeValue = value ?: run {
        return false
    }

    return attributeValue.toString() in dataValue.toString()
}

internal fun conditionEvaluatorsNotContains(data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, attribute: String, value: @Serializable(with = AnyValueSerializer::class) Any?): Boolean {
    Log.i("Rownd", "Condition: NOT_CONTAINS")
    val dataValue = data[attribute] ?: run {
        return false
    }
    val attributeValue = value ?: run {
        return false
    }

    return attributeValue.toString() !in dataValue.toString()
}

internal fun conditionEvaluatorsIn(data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, attribute: String, value: @Serializable(with = AnyValueSerializer::class) Any?): Boolean {
    Log.i("Rownd", "Condition: IN")
    val dataValue = data[attribute] ?: run {
        return false
    }
    val attributeValue = value ?: run {
        return false
    }

    return dataValue.toString() in attributeValue.toString()
}

internal fun conditionEvaluatorsNotIn(data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, attribute: String, value: @Serializable(with = AnyValueSerializer::class) Any?): Boolean {
    Log.i("Rownd", "Condition: NOT_IN")
    val dataValue = data[attribute] ?: run {
        return false
    }
    val attributeValue = value ?: run {
        return false
    }

    return  dataValue.toString() !in attributeValue.toString()
}

internal fun conditionEvaluatorsExists(data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, attribute: String, value: @Serializable(with = AnyValueSerializer::class) Any?): Boolean {
    Log.i("Rownd", "Condition: EXISTS")
    return data[attribute] != null
}

internal fun conditionEvaluatorsNotExists(data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, attribute: String, value: @Serializable(with = AnyValueSerializer::class) Any?): Boolean {
    Log.i("Rownd", "Condition: NOT_EXISTS")
    return data[attribute] == null
}

internal val conditionEvaluators: List<Pair<AutomationRuleCondition, (Map<String, @Serializable(with = AnyValueSerializer::class) Any?>, String, @Serializable(with = AnyValueSerializer::class) Any?) -> Boolean>> = listOf(
    Pair(AutomationRuleCondition.Equals) { x, y, z -> conditionEvaluatorsEquals(x, y, z) },
    Pair(AutomationRuleCondition.NotEquals) { x, y, z -> conditionEvaluatorsNotEquals(x, y, z) },
    Pair(AutomationRuleCondition.Contains) { x, y, z -> conditionEvaluatorsContains(x, y, z) },
    Pair(AutomationRuleCondition.NotContains) { x, y, z -> conditionEvaluatorsNotContains(x, y, z) },
    Pair(AutomationRuleCondition.In) { x, y, z -> conditionEvaluatorsIn(x, y, z) },
    Pair(AutomationRuleCondition.NotIn) { x, y, z -> conditionEvaluatorsNotIn(x, y, z) },
    Pair(AutomationRuleCondition.Exists) { x, y, z -> conditionEvaluatorsExists(x, y, z) },
    Pair(AutomationRuleCondition.NotExists) { x, y, z -> conditionEvaluatorsNotExists(x, y, z) },
)
