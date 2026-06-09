package sm.selflearn.samskrtam.quiz.event;

public enum OutboxEventType {
    ANSWER_SUBMITTED,
    SESSION_COMPLETED,
    PROFILE_UPDATED, // Added for user-service
    USER_BLOCKED,    // Added for user-service
    USER_UNBLOCKED   // Added for user-service
}
