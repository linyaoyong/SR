package com.share.rental.common.response;

import java.util.List;

public record PageResult<T>(List<T> records, long total, int page, int size, long pages) {

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than or equal to 1");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must be greater than or equal to 0");
        }
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(List.copyOf(records), total, page, size, pages);
    }
}
