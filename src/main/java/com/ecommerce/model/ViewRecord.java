package com.ecommerce.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ViewRecord(long userId, long productId, LocalDateTime viewedAt) implements Serializable {
}
