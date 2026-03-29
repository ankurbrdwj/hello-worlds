package com.ankur.bdd.result;

import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E> s -> Result.success(mapper.apply(s.value()));
            case Failure<T, E> f -> Result.failure(f.error());
        };
    }

    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E> s -> mapper.apply(s.value());
            case Failure<T, E> f -> Result.failure(f.error());
        };
    }

    default <U> U fold(Function<T, U> onSuccess, Function<E, U> onError) {
        return switch (this) {
            case Success<T, E> s -> onSuccess.apply(s.value());
            case Failure<T, E> f -> onError.apply(f.error());
        };
    }

    default boolean isSuccess() { return this instanceof Success; }
    default boolean isFailure() { return this instanceof Failure; }

    default Result<T, E> peekError(Consumer<E> consumer) {
        if (this instanceof Failure<T, E> f) consumer.accept(f.error());
        return this;
    }
}