// AI Provenance: generated from docs/00-internal-coding-standards.md §3, docs/02-api-spec.md §2
package com.gpc.oms.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        java.util.Objects.requireNonNull(page, "page must not be null");
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
