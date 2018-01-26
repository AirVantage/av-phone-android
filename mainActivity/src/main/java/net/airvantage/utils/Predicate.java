package net.airvantage.utils;

public interface Predicate<T> {
    boolean matches(T item);
}
