package net.airvantage.model

open class Data(var id: String, private val label: String, private val elementType: String) {
    var type: String? = null
    var data: MutableList<Data>? = null
}
