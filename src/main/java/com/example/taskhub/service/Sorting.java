package com.example.taskhub.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.taskhub.exception.ApiException;

/**
 * Validates pageable sort properties against an allow-list.
 *
 * <p>Spring Data happily builds a query for any property name it is handed. An unknown one
 * (for example {@code ?sort=bogus}) only blows up deeper down, surfacing as a 500. Rejecting
 * it here turns that into a clear, self-documenting 400.
 */
public final class Sorting {
    private Sorting() {}

    public static void validate(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (!allowed.contains(property)) {
                throw ApiException.badRequest("Cannot sort by '%s'. Allowed fields: %s"
                        .formatted(property, allowed.stream().sorted().collect(Collectors.joining(", "))));
            }
        }
    }
}
