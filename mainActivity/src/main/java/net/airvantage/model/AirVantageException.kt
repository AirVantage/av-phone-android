package net.airvantage.model

class AirVantageException(error: net.airvantage.model.AvError) : Exception() {
    var error: AvError? = null

    init {
        this.error = error
    }

    companion object {

        private const val serialVersionUID = 1L
    }
}
