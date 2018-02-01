package net.airvantage.model.alert.v1

class Alert {

    var date: Long = 0
    var uid: String? = null

    class Rule {
        var name: String? = null
        var message: String? = null
        var uid: String? = null
    }
}
