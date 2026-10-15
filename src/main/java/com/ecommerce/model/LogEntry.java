package com.ecommerce.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record LogEntry(long id, LocalDateTime time, String actor, String message) implements Serializable {
}
