<!--
Role: Senior Engineer. Task: PATCH /api/v1/workorders/{id}/status
Context files: docs/domain-model.md, docs/api-spec.md, docs/api-rules.md
Constraints: One-way state machine rule, HTTP 422 mapping RFC 7807 on invalid state.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: PATCH /api/v1/workorders/{id}/status

```java
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")
    public ResponseEntity<WorkOrderResponse> updateStatus(
            @PathVariable UUID id, 
            @Valid @RequestBody StatusUpdateRequest req) {
        
        log.info("update status workorderId={}", id);
        
        WorkOrder wo = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order Not Found"));
            
        try {
            wo.advanceStatus(req.status()); // Quy tắc bất biến được đóng gói trong Entity
        } catch (IllegalStateException ex) {
            // Theo api-rules.md, exception này sẽ được @ControllerAdvice bắt 
            // và map thành JSON RFC 7807 (status 422 Unprocessable Entity)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }
        
        return ResponseEntity.ok(WorkOrderResponse.from(repo.save(wo)));
    }
    
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record StatusUpdateRequest(
        @NotNull WorkOrderStatus status
    ) {}
```
