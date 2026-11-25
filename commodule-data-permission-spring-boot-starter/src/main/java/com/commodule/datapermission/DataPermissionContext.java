package com.commodule.datapermission;

import java.util.Optional;

public final class DataPermissionContext {

    private static final ThreadLocal<DataPermissionContext> CURRENT = new ThreadLocal<>();

    private final String resource;
    private final DataPermissionDecision decision;

    private DataPermissionContext(String resource, DataPermissionDecision decision) {
        this.resource = resource;
        this.decision = decision;
    }

    public String resource() {
        return resource;
    }

    public DataPermissionDecision decision() {
        return decision;
    }

    public static Optional<DataPermissionContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Scope open(String resource, DataPermissionDecision decision) {
        DataPermissionContext previous = CURRENT.get();
        CURRENT.set(new DataPermissionContext(resource, decision));
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {

        private final DataPermissionContext previous;

        private Scope(DataPermissionContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}

