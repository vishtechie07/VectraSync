package com.vectrasync.crm;

import java.util.Optional;

public record UpsertResult(Status status, String id, String error) {

    public enum Status { CREATED, UPDATED, SKIPPED, FAILED }

    public static UpsertResult created(String id) {
        return new UpsertResult(Status.CREATED, id, null);
    }

    public static UpsertResult updated(String id) {
        return new UpsertResult(Status.UPDATED, id, null);
    }

    public static UpsertResult failed(String error) {
        return new UpsertResult(Status.FAILED, null, error);
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(error);
    }
}
