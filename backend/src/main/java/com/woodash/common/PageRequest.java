package com.woodash.common;

public record PageRequest(int page, int size) {

    public static final int DEFAULT_SIZE = 25;
    public static final int MAX_SIZE = 100;

    public static PageRequest of(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE || (long) page * size > Integer.MAX_VALUE) {
            ErrorCode.INVALID_REQUEST_BODY.throwException();
        }
        return new PageRequest(page, size);
    }
}
