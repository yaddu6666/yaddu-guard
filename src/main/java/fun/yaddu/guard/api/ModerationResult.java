package fun.yaddu.guard.api;

public class ModerationResult {

    private final boolean toxic;
    private final int severity;
    private final String reason;
    private final String category;
    private final boolean error;

    public ModerationResult(boolean toxic, int severity, String reason, String category) {
        this.toxic = toxic;
        this.severity = severity;
        this.reason = reason;
        this.category = category;
        this.error = false;
    }

    // Error result constructor
    public ModerationResult(String errorReason) {
        this.toxic = false;
        this.severity = 0;
        this.reason = errorReason;
        this.category = "error";
        this.error = true;
    }

    public boolean isToxic() { return toxic; }
    public int getSeverity() { return severity; }
    public String getReason() { return reason; }
    public String getCategory() { return category; }
    public boolean isError() { return error; }

    @Override
    public String toString() {
        return "ModerationResult{toxic=" + toxic + ", severity=" + severity 
            + ", reason='" + reason + "', category='" + category + "'}";
    }
}
