package net.airvantage.model.alert.v2.alertrule

import net.airvantage.utils.Predicate


class AlertRule {

    var id: String? = null
    var active: Boolean = false

    var message: String? = null
    var name: String? = null
    var targetType: String? = null

    var conditions: MutableList<Condition>? = null
    var metadata: MutableMap<String, Any>? = null

    companion object {

        fun isNamed(name: String): Predicate<AlertRule> {
            return  { item -> name == item.name }
        }
    }
}
