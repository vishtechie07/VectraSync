package com.vectrasync.core;

import java.util.Optional;

@FunctionalInterface
public interface KeyResolver {
    Optional<String> resolve();
}
