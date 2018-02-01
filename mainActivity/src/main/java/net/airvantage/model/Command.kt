package net.airvantage.model

class Command(id: String, label: String) : Data(id, label, "command") {

    var parameters: List<Parameter>? = null

}
