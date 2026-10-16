package com.example.taskhub.unit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.taskhub.exception.ApiException;
import com.example.taskhub.service.Sorting;

class SortingTest {

    private static final Set<String> ALLOWED = Set.of("id", "title", "createdAt");

    @Test
    void acceptsEveryAllowedPropertyInAnyDirection() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("title").descending().and(Sort.by("createdAt")));

        assertThatCode(() -> Sorting.validate(pageable, ALLOWED)).doesNotThrowAnyException();
    }

    @Test
    void acceptsAnUnsortedPageable() {
        assertThatCode(() -> Sorting.validate(PageRequest.of(0, 10), ALLOWED)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnknownPropertyWithAHelpful400() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("password"));

        assertThatThrownBy(() -> Sorting.validate(pageable, ALLOWED))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("password")
                .hasMessageContaining("title");
    }
}
