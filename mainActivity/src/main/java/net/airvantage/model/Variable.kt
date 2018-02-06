package net.airvantage.model

class Variable(id: String, label: String, type: String) : Data(id, label, "variable") {

    init {
        this.type = type
    }
}
