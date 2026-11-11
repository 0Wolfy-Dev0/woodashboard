package com.woodash.common;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        int totalPages = size > 0 ? (int) ((total + size - 1) / size) : 0;
        return new PageResponse<>(items, page, size, total, totalPages);
    }

    public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = items.stream().<R>map(mapper).toList();
        return new PageResponse<>(mapped, page, size, total, totalPages);
    }
}
