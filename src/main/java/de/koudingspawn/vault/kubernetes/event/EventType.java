package de.koudingspawn.vault.kubernetes.event;

public enum EventType {
    CREATION_SUCCESSFUL("Normal", "SuccessfulCreated"),
    CREATION_FAILED("Failure", "FailedCreation"),
    MODIFICATION_SUCCESSFUL("Normal", "SuccessfulModified"),
    MODIFICATION_FAILED("Failure", "FailedModification"),
    ROTATION("Rotation", "RotationTriggered"),
    FIXED_REFERENCE("Normal", "FixedOwnerReference"),
    DELETION("Normal", "DeletionOfResource");

    private final String type;
    private final String reason;

    private EventType(String type, String reason) {
        this.type = type;
        this.reason = reason;
    }

    public String getEventType() {
        return type;
    }

    public String getReason() {
        return reason;
    }
}
