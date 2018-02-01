package net.airvantage.utils.alert

interface IAlertAdapterFactoryListener {
    fun alertAdapterAvailable(adapter: DefaultAlertAdapter)
}
