package com.ankur.price_alert.result;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result<T, E> — a sealed type that explicitly represents either success or failure.
 *
 * Why use this instead of exceptions?
 *   - Errors become part of the method signature (callers can't ignore them)
 *   - No hidden control flow jumps; every failure path is visible
 *   - Composable: chain operations with map/flatMap just like Optional or Stream
 *
 * Core shapes:
 *   Success<T, E>  — wraps a value T
 *   Failure<T, E>  — wraps an error E
 *
 * @param <T> the success value type
 * @param <E> the error type
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    // ─── Inner records ────────────────────────────────────────────────────────

    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    // ─── Smart constructors ───────────────────────────────────────────────────

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    // ─── Transformation ───────────────────────────────────────────────────────

    /**
     * map — transform the success value; failures pass through unchanged.
     *
     * Think of it like Stream.map: apply a pure function to the happy-path value.
     *
     * Success(user)  .map(User::getEmail)  →  Success("alice@example.com")
     * Failure(error) .map(User::getEmail)  →  Failure(error)          ← untouched
     */
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E> s -> Result.success(mapper.apply(s.value()));
            case Failure<T, E> f -> Result.failure(f.error());
        };
    }

    /**
     * mapError — transform the error type; successes pass through unchanged.
     *
     * Useful for converting domain errors into HTTP/API error shapes at the edge.
     *
     * Failure(UserError.NotFound) .mapError(e -> new ApiError(404, e.message()))
     *   →  Failure(ApiError(404, "User not found: 42"))
     */
    default <U> Result<T, U> mapError(Function<E, U> errorMapper) {
        return switch (this) {
            case Success<T, E> s -> Result.success(s.value());
            case Failure<T, E> f -> Result.failure(errorMapper.apply(f.error()));
        };
    }

    /**
     * flatMap — chain a Result-returning operation on success.
     *
     * Unlike map (which wraps the result automatically), flatMap expects the
     * mapper to return a Result itself — this prevents double-wrapping and lets
     * you sequence steps that can each independently fail.
     *
     * validateEmail(email)                         // Result<String, UserError>
     *   .flatMap(e -> checkNotDuplicate(e))         // Result<String, UserError>
     *   .flatMap(e -> persist(user))                // Result<User,   UserError>
     */
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E> s -> mapper.apply(s.value());
            case Failure<T, E> f -> Result.failure(f.error());
        };
    }

    // ─── Recovery ─────────────────────────────────────────────────────────────

    /**
     * recover — handle a failure and potentially return a new Result.
     *
     * The recovery function receives the error and can either:
     *   • return Result.success(fallback)  — recovered successfully
     *   • return Result.failure(newError)  — replaced with a different error
     *
     * Failure(UserError.NotFound("guest"))
     *   .recover(e -> createGuestUser())           →  Success(guestUser)
     */
    default Result<T, E> recover(Function<E, Result<T, E>> recovery) {
        return switch (this) {
            case Success<T, E> s -> this;
            case Failure<T, E> f -> recovery.apply(f.error());
        };
    }

    /**
     * recoverWith — recover from failure with a plain value (not a Result).
     *
     * Shorthand for recover(e -> Result.success(fallback)).
     *
     * Failure(UserError.NotFound) .recoverWith(e -> User.anonymous())
     *   →  Success(User.anonymous())
     */
    default Result<T, E> recoverWith(Function<E, T> recovery) {
        return switch (this) {
            case Success<T, E> s -> this;
            case Failure<T, E> f -> Result.success(recovery.apply(f.error()));
        };
    }

    // ─── Pattern matching / extraction ────────────────────────────────────────

    /**
     * fold — collapse a Result<T,E> into a single value U.
     *
     * This is the terminal operation — the point where you stop composing and
     * actually produce a response (e.g. ResponseEntity in a controller).
     *
     * result.fold(
     *   user  -> ResponseEntity.ok(UserResponse.fromUser(user)),
     *   error -> ResponseEntity.status(toStatus(error)).body(error.message())
     * )
     */
    default <U> U fold(Function<T, U> onSuccess, Function<E, U> onError) {
        return switch (this) {
            case Success<T, E> s -> onSuccess.apply(s.value());
            case Failure<T, E> f -> onError.apply(f.error());
        };
    }

    // ─── Side effects / debugging ─────────────────────────────────────────────

    /**
     * peekSuccess — run a side-effect on success (logging, metrics) without
     * interrupting the chain. Returns the original Result unchanged.
     */
    default Result<T, E> peekSuccess(Consumer<T> consumer) {
        if (this instanceof Success<T, E> s) {
            consumer.accept(s.value());
        }
        return this;
    }

    /**
     * peekError — run a side-effect on failure (logging, alerting) without
     * interrupting the chain. Returns the original Result unchanged.
     */
    default Result<T, E> peekError(Consumer<E> consumer) {
        if (this instanceof Failure<T, E> f) {
            consumer.accept(f.error());
        }
        return this;
    }

    // ─── State checks ─────────────────────────────────────────────────────────

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }
}