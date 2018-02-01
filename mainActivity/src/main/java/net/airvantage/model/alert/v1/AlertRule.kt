package net.airvantage.model.alert.v1


class AlertRule {
    var uid: String? = null
    var active = true
    var name: String? = null
    var eventType: String? = null
    var conditions: MutableList<Condition>? = null



    companion object {

        fun isNamed(name: String): net.airvantage.utils.Predicate<AlertRule> {
            return  { item -> name == item.name }
        }
    }
}
