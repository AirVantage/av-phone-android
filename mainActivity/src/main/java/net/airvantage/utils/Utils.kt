package net.airvantage.utils


typealias Predicate<T> = (T) -> Boolean

object Utils {

    fun <T> first(list: List<T>?): T? {
        return if (list == null || list.isEmpty()) null else list[0]
    }

    fun <T> firstWhere(list: List<T>?, predicate: Predicate<T>): T? {

        if (list != null) {
            for (item in list) {
                val match = predicate.invoke(item)
                if (match) {
                    return item
                }
            }
        }
        return null
    }

}
