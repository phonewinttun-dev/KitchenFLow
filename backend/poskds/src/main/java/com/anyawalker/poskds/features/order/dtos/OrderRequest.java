package com.anyawalker.poskds.features.order.dtos;

import java.util.List;

public record OrderRequest( List<OrderItemRequest> orderItems) {
}
