package com.restaurantapp.demo.util;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * PublicCodeGenerator
 *
 * Utility to produce human-friendly public codes for entities (orders, tables, reservations).
 *
 * Notes for developers:
 * - The generator produces codes in the format PREFIX-#### (e.g. ORD-0001, TAB-0010, RSV-0123).
 * - The existsCheck predicate must return true when a candidate already exists in persistence
 *   (e.g. repository.existsByPublicCode(candidate)). The method will loop until a non-existing
 *   candidate is found. Because this is not performed inside a DB transaction with unique index
 *   protection, a race condition is still possible; callers should rely on a unique DB constraint
 *   and handle duplicates at the repository/DB layer as needed.
 * - The methods are thread-safe (stateless) but not atomic across DB checks.
 * - A compatibility method with a historical typo is provided but deprecated — prefer
 *   generateReservationCode(...).
 */
public final class PublicCodeGenerator {

    private PublicCodeGenerator() {
        // utility
    }

    /**
     * Generate an order public code with prefix "ORD".
     *
     * @param start       numeric seed (usually sequence or index)
     * @param existsCheck predicate that returns true when a candidate already exists
     */
    public static String generateOrderCode(long start, Predicate<String> existsCheck) {
        return generate("ORD", start, existsCheck);
    }

    /**
     * Generate a table public code with prefix "TAB".
     *
     * @param start       numeric seed (usually sequence or index)
     * @param existsCheck predicate that returns true when a candidate already exists
     */
    public static String generateTableCode(long start, Predicate<String> existsCheck) {
        return generate("TAB", start, existsCheck);
    }

    /**
     * Generate a reservation public code with prefix "RSV".
     *
     * @param start       numeric seed (usually sequence or index)
     * @param existsCheck predicate that returns true when a candidate already exists
     */
    public static String generateReservationCode(long start, Predicate<String> existsCheck) {
        return generate("RSV", start, existsCheck);
    }

    /**
     * Legacy misspelled method kept for backwards compatibility. Prefer generateReservationCode.
     *
     * @deprecated Use {@link #generateReservationCode(long, Predicate)} instead.
     */
    @Deprecated
    public static String generateReservaionCode(long start, Predicate<String> existsCheck) {
        return generateReservationCode(start, existsCheck);
    }

    /**
     * Core generator implementation. Produces codes like PREFIX-0001.
     *
     * @param prefix      non-null, non-blank prefix (e.g. "ORD")
     * @param start       starting sequence value (will be coerced to >= 1)
     * @param existsCheck predicate used to test uniqueness
     * @return a unique candidate that does not satisfy existsCheck
     */
    private static String generate(String prefix, long start, Predicate<String> existsCheck) {
        Objects.requireNonNull(prefix, "prefix is required");
        if (prefix.isBlank()) throw new IllegalArgumentException("prefix must not be blank");
        Objects.requireNonNull(existsCheck, "existsCheck predicate is required");

        long next = Math.max(start, 1);
        String candidate;
        // Loop until a candidate passes the uniqueness check. Keep the format zero-padded.
        do {
            candidate = String.format("%s-%04d", prefix, next++);
            // Note: callers should handle potential long loops. In practice the start value should
            // come from a sequence/row count to avoid large iteration counts.
        } while (existsCheck.test(candidate));

        return candidate;
    }
}
