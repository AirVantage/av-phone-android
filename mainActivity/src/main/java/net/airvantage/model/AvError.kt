package net.airvantage.model

class AvError {

    var error: String
    var errorParameters: List<String>

    constructor(error: String) {
        this.error = error
        this.errorParameters = emptyList()
    }

    constructor(error: String, errorParameters: List<String>) : super() {
        this.error = error
        this.errorParameters = errorParameters
    }

    fun systemAlreadyExists(): Boolean {
        return SYSTEM_EXISTS == error || GATEWAY_EXISTS == error
    }

    fun gatewayAlreadyExists(): Boolean {
        return GATEWAY_ASSIGNED == error
    }

    fun applicationAlreadyUsed(): Boolean {
        return APPLICATION_TYPE_EXISTS == error
    }

    fun tooManyAlerRules(): Boolean {
        return ALERT_RULES_TOO_MANY == error
    }

    fun forbidden(): Boolean {
        return FORBIDDEN == error
    }

    fun cantCreateApplication(): Boolean {
        return isForbiddenAction("POST", "application")
    }

    fun cantCreateSystem(): Boolean {
        return isForbiddenAction("POST", "system")
    }

    fun cantCreateAlertRule(): Boolean {
        return isForbiddenAction("POST", "alerts/rules")
    }

    fun cantUpdateApplication(): Boolean {
        return isForbiddenAction("PUT", "application")
    }

    fun cantUpdateSystem(): Boolean {
        return isForbiddenAction("PUT", "system")
    }

    private fun isForbiddenAction(method: String, entity: String): Boolean {
        return if (forbidden()) {
            val requestMethod = errorParameters[0]
            val requestUrl = errorParameters[1]
            method.equals(requestMethod, ignoreCase = true) && requestUrl.contains(entity)
        } else {
            false
        }
    }

    fun missingRights(): Boolean {
        return MISSING_RIGHTS == error
    }

    companion object {

        private const val SYSTEM_EXISTS = "system.not.unique.identifiers"
        private const val GATEWAY_EXISTS = "gateway.not.unique.identifiers"
        private const val GATEWAY_ASSIGNED = "gateway.assigned"
        const val FORBIDDEN = "forbidden"
        private const val APPLICATION_TYPE_EXISTS = "application.type.already.used"
        private const val ALERT_RULES_TOO_MANY = "alert.rule.too.many"
        const val MISSING_RIGHTS = "missing.rights"
    }

}
