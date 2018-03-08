package com.commercetools.util.function;

import java.util.function.Function;

/**
 * Similar to {@link java.util.function.BiFunction}:
 * <p>
 * Represents a function that accepts tree arguments and produces a result.
 * This is the tree-arity specialization of {@link Function}.
 * <p>
 * This is a functional interface whose functional method is {@link #apply(Object, Object, Object)}.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <V> the type of the third argument to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);
}
