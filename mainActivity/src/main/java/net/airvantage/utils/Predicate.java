package net.airvantage.utils;

public interface Predicate<T> {
    public boolean matches(T item);
}
