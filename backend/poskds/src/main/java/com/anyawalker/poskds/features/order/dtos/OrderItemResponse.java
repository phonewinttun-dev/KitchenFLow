package com.anyawalker.poskds.features.order.dtos;

public record OrderItemResponse(Long id,Long menuId,String menuName,int quantity,int unitPrice,int totalPrice) {
}
