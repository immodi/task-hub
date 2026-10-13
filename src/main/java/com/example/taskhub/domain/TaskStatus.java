package com.example.taskhub.domain;

public enum TaskStatus {
    TODO, IN_PROGRESS, BLOCKED, DONE;

    /** Tiny workflow rule: you can't jump straight from TODO to DONE, etc. */
    public boolean canTransitionTo(TaskStatus next) {
        if (this == next) {
            return true;
        }
        return switch (this) {
            case TODO -> next == IN_PROGRESS || next == BLOCKED;
            case IN_PROGRESS -> true;
            case BLOCKED -> next == IN_PROGRESS || next == TODO;
            case DONE -> next == TODO || next == IN_PROGRESS;
        };
    }
}
