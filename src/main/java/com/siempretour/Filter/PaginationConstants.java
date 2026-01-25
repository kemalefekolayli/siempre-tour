package com.siempretour.Filter;



public final class PaginationConstants {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 30;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    private PaginationConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates and normalizes page size to ensure it doesn't exceed MAX_PAGE_SIZE
     */
    public static int normalizePageSize(int requestedSize) {
        if (requestedSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }

    /**
     * Validates page number to ensure it's non-negative
     */
    public static int normalizePageNumber(int requestedPage) {
        return Math.max(requestedPage, 0);
    }
}