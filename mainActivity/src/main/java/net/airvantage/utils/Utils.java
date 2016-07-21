package net.airvantage.utils;

import java.util.List;

public class Utils {

    public static <T> T first(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    public static <T> T firstWhere(List<T> list, Predicate<T> predicate) {
        
        if (list != null) {
            for (T item : list) {
                if (predicate.matches(item)) {
                    return item;
                }
            }
        }
        return null;
    }
    
}
