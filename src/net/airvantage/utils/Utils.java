package net.airvantage.utils;

import java.util.List;

public class Utils {

    public static <T> T first(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

}
