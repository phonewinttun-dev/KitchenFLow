package com.anyawalker.poskds.features.order.dtos;

import java.time.LocalDateTime;

public record OrderCancelResponse(Long orderId, Long userId, String status, LocalDateTime resolvedAt) {
}
