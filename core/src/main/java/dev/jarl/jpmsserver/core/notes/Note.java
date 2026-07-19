package dev.jarl.jpmsserver.core.notes;

import java.time.OffsetDateTime;

public record Note(long id, String title, String body, OffsetDateTime createdAt) {}
