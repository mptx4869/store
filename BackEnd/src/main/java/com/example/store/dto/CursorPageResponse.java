package com.example.store.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Generic response wrapper for keyset (cursor) pagination.
 *
 * <p>Clients pass {@code nextLastId} + {@code nextLastCreatedAt} on the next
 * request to continue from where the previous page ended.  When
 * {@code hasNext} is {@code false} there are no more pages and both cursor
 * fields are {@code null}.
 *
 * <p>Sort is always {@code created_at DESC, id DESC} (stable, deterministic).
 */
@Getter
@Builder
public class CursorPageResponse<T> {

    private List<T> content;

    /** True when there is at least one more page after this one. */
    private boolean hasNext;

    /**
     * {@code id} of the last item in {@code content}; pass as {@code lastId}
     * on the next request.  {@code null} when {@code hasNext} is false.
     */
    private Long nextLastId;

    /**
     * {@code createdAt} of the last item in {@code content}; pass as
     * {@code lastCreatedAt} on the next request.  {@code null} when
     * {@code hasNext} is false.
     */
    private LocalDateTime nextLastCreatedAt;
}
