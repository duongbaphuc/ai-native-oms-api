// AI Provenance: generated from docs/api-spec.md §5, docs/api-rules.md §2
package com.gpc.oms.exception;

import java.net.URI;

/**
 * Centralized RFC 7807 Problem Detail Type URIs.
 * Pre-allocated immutable URI constants to optimize JVM performance and eliminate magic strings.
 */
public final class ProblemTypes {

    private ProblemTypes() {
        // Enforce non-instantiability (Effective Java Item 4)
    }

    public static final URI VALIDATION_ERROR = URI.create("urn:problem-type:validation-error");
    public static final URI MALFORMED_JSON = URI.create("urn:problem-type:malformed-json");
    public static final URI UNAUTHORIZED = URI.create("urn:problem-type:unauthorized");
    public static final URI FORBIDDEN = URI.create("urn:problem-type:forbidden");
    public static final URI NOT_FOUND = URI.create("urn:problem-type:not-found");
    public static final URI INVALID_STATE_TRANSITION = URI.create("urn:problem-type:invalid-state-transition");
    public static final URI INTERNAL_ERROR = URI.create("urn:problem-type:internal-error");
}
