package com.firstspringproject.onlinecodeexecuter.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "code_requests", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class CodeExecutionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "language", length = 20)
    private String language = "java";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Execution metrics
    @Column(name = "memory_used_kb")
    private Long memoryUsedKb;

    @Column(name = "line_count")
    private Integer lineCount;

    @Column(name = "character_count")
    private Integer characterCount;

    // User session info (for future features)
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Constructors
    public CodeExecutionRequest() {
        this.createdAt = LocalDateTime.now();
        this.status = ExecutionStatus.PENDING;
        this.language = "java";
    }

    public CodeExecutionRequest(String code) {
        this();
        this.code = code;
        calculateMetrics();
    }

    public CodeExecutionRequest(String code, String sessionId) {
        this();
        this.code = code;
        this.sessionId = sessionId;
        calculateMetrics();
    }

    // Utility methods
    public void calculateMetrics() {
        if (this.code != null) {
            this.lineCount = this.code.split("\r\n|\r|\n").length;
            this.characterCount = this.code.length();
        }
    }

    public void markAsProcessing() {
        this.status = ExecutionStatus.PROCESSING;
        this.executedAt = LocalDateTime.now();
    }

    public void markAsCompleted(String output, Long executionTimeMs) {
        this.status = ExecutionStatus.COMPLETED;
        this.output = output;
        this.executionTimeMs = executionTimeMs;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorDetails, Long executionTimeMs) {
        this.status = ExecutionStatus.FAILED;
        this.errorDetails = errorDetails;
        this.executionTimeMs = executionTimeMs;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsTimeout() {
        this.status = ExecutionStatus.TIMEOUT;
        this.errorDetails = "Execution timeout: Code took too long to execute";
        this.completedAt = LocalDateTime.now();
    }

    // Formatting methods for UI
    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getFormattedExecutionTime() {
        if (executionTimeMs == null) return "N/A";
        if (executionTimeMs < 1000) {
            return executionTimeMs + "ms";
        } else {
            return String.format("%.2fs", executionTimeMs / 1000.0);
        }
    }

    public String getFormattedCompletedAt() {
        return completedAt != null ? completedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
    }

    public String getStatusEmoji() {
        switch (status) {
            case PENDING: return "⏳";
            case PROCESSING: return "⚡";
            case COMPLETED: return "✅";
            case FAILED: return "❌";
            case TIMEOUT: return "⏰";
            default: return "❓";
        }
    }

    public String getStatusColor() {
        switch (status) {
            case PENDING: return "#f39c12";    // Orange
            case PROCESSING: return "#3498db"; // Blue
            case COMPLETED: return "#27ae60";  // Green
            case FAILED: return "#e74c3c";     // Red
            case TIMEOUT: return "#9b59b6";    // Purple
            default: return "#95a5a6";         // Gray
        }
    }

    public boolean isCompleted() {
        return status == ExecutionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ExecutionStatus.FAILED || status == ExecutionStatus.TIMEOUT;
    }

    public boolean isProcessing() {
        return status == ExecutionStatus.PROCESSING;
    }

    public boolean isPending() {
        return status == ExecutionStatus.PENDING;
    }

    // Performance analysis
    public String getPerformanceCategory() {
        if (executionTimeMs == null) return "Unknown";
        if (executionTimeMs < 100) return "Excellent";
        if (executionTimeMs < 500) return "Good";
        if (executionTimeMs < 2000) return "Average";
        return "Slow";
    }

    public double getExecutionTimeInSeconds() {
        return executionTimeMs != null ? executionTimeMs / 1000.0 : 0.0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) {
        this.code = code;
        calculateMetrics();
    }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getMemoryUsedKb() { return memoryUsedKb; }
    public void setMemoryUsedKb(Long memoryUsedKb) { this.memoryUsedKb = memoryUsedKb; }

    public Integer getLineCount() { return lineCount; }
    public void setLineCount(Integer lineCount) { this.lineCount = lineCount; }

    public Integer getCharacterCount() { return characterCount; }
    public void setCharacterCount(Integer characterCount) { this.characterCount = characterCount; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    // Enum for status management
    public enum ExecutionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        TIMEOUT
    }

    // toString for debugging
    @Override
    public String toString() {
        return "CodeExecutionRequest{" +
                "id=" + id +
                ", status=" + status +
                ", language='" + language + '\'' +
                ", lineCount=" + lineCount +
                ", executionTimeMs=" + executionTimeMs +
                ", createdAt=" + getFormattedCreatedAt() +
                '}';
    }

    // Builder pattern for fluent creation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String code;
        private String sessionId;
        private String userAgent;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public CodeExecutionRequest build() {
            CodeExecutionRequest request = new CodeExecutionRequest();
            request.setCode(code);
            request.setSessionId(sessionId);
            request.setUserAgent(userAgent);
            return request;
        }
    }
}