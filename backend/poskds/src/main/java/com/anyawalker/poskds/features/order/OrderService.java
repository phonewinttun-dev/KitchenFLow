package com.anyawalker.poskds.features.order;

import com.anyawalker.poskds.features.menu.MenuService;
import com.anyawalker.poskds.features.order.dtos.*;
import com.anyawalker.poskds.features.order.exceptions.OrderFailureException;
import com.anyawalker.poskds.models.dtos.OrderStatus;
import com.anyawalker.poskds.models.entities.MenuEntity;
import com.anyawalker.poskds.models.entities.OrderEntity;
import com.anyawalker.poskds.models.entities.OrderItemEntity;
import com.anyawalker.poskds.models.entities.UserEntity;
import com.anyawalker.poskds.repos.OrderRepo;
import com.anyawalker.poskds.repos.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final MenuService menuService;

    public OrderService(OrderRepo orderRepo, UserRepo userRepo, MenuService menuService) {
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
        this.menuService = menuService;
    }

    public List<OrderResponse> viewAllOrders() {

        return orderRepo.findAll().stream().map(
                orderEntity -> new OrderResponse(
                        orderEntity.getId(),
                        orderEntity.getUserEntity().getId(),
                        orderEntity.getStatus(),
                        "",
                        orderEntity.getOrderItemEntityList().stream().map(
                                orderItemEntity -> new OrderItemResponse(
                                        orderItemEntity.getId(),
                                        orderItemEntity.getMenuEntity().getId(),
                                        orderItemEntity.getMenuEntity().getName(),
                                        orderItemEntity.getQuantity(),
                                        orderItemEntity.getUnitPrice(),
                                        orderItemEntity.getTotalPrice())

                        ).toList(), orderEntity.getTotalPrice()
                )
        ).toList();
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, Long userId) {
        UserEntity orderCreator = userRepo.findById(userId)
                .orElseThrow(() -> new OrderFailureException("Could not create the order due to invalid user id"));

        OrderEntity order = new OrderEntity();
        order.setUserEntity(orderCreator);
        order.setStatus(OrderStatus.WAITING.getValue());
        //extract all menu id
        List<Long> menuEntityIds = orderRequest.orderItems()
                .stream()
                .map(OrderItemRequest::menuId)
                .distinct()
                .toList();

        Map<Long, MenuEntity> menuEntityMap = menuService.getMenuEntityMapByIds(menuEntityIds);

        //orderItemRequest -> orderItemEntity mapping process
        AtomicInteger totalOrderPrice = new AtomicInteger();
        List<OrderItemEntity> orderItemList = orderRequest.orderItems()
                .stream()
                .map(orderItemRequest -> {
                    MenuEntity menuEntity = menuEntityMap.get(orderItemRequest.menuId());

                    if (menuEntity == null)
                        throw new OrderFailureException("Could not create the order due to invalid menu_id with " +
                                orderItemRequest.menuId());

                    int totalPrice = orderItemRequest.quantity() * menuEntity.getCurrentPrice();
                    totalOrderPrice.addAndGet(totalPrice);

                    OrderItemEntity orderItem = new OrderItemEntity();
                    orderItem.setMenuEntity(menuEntity);
                    orderItem.setTotalPrice(totalPrice);
                    orderItem.setUnitPrice(menuEntity.getCurrentPrice());
                    orderItem.setQuantity(orderItemRequest.quantity());
                    orderItem.setOrderEntity(order);
                    return orderItem;
                })
                .toList();
        order.setOrderItemEntityList(orderItemList);
        order.setTotalPrice(totalOrderPrice.get());
        OrderEntity savedOrder = orderRepo.save(order);

        //map orderItemEntity to orderItemResponse to get the db generated id
        List<OrderItemResponse> orderItemResponses = savedOrder.getOrderItemEntityList()
                .stream()
                .map(
                        orderItemEntity -> new OrderItemResponse(
                                orderItemEntity.getId(),
                                orderItemEntity.getMenuEntity().getId(),
                                orderItemEntity.getMenuEntity().getName(),
                                orderItemEntity.getQuantity(),
                                orderItemEntity.getUnitPrice(),
                                orderItemEntity.getTotalPrice()
                        )
                ).toList();

        return new OrderResponse(
                savedOrder.getId(),
                userId,
                savedOrder.getStatus(),
                "order created successfully",
                orderItemResponses,savedOrder.getTotalPrice());

    }

    @Transactional
    public OrderCancelResponse cancelOrder(Long orderId, Long userId) {

        OrderEntity orderEntity = orderRepo.
                findByIdAndUserEntity_Id(orderId, userId)
                .orElseThrow(() ->
                        new OrderFailureException("Order doesn't exist")
                );

        if (orderEntity.getStatus().equals(OrderStatus.CANCEL.getValue()))
            throw new OrderFailureException("Order is already canceled");

        orderEntity.setResolvedAt(LocalDateTime.now());
        orderEntity.setStatus(OrderStatus.CANCEL.getValue());

        return new OrderCancelResponse(orderEntity.getId(), userId, orderEntity.getStatus(), orderEntity.getResolvedAt());
    }

    @Transactional
    public OrderResponse updateOrderItems(Long orderId,
                                          List<OrderItemUpdateRequest> orderItemUpdateRequests,
                                          Long userId) {

        OrderEntity orderEntity = orderRepo.
                findByIdAndUserEntity_Id(orderId, userId)
                .orElseThrow(() ->
                        new OrderFailureException("Order doesn't exist")
                );

        if (!orderEntity.getStatus().equals(OrderStatus.WAITING.getValue()))
            throw new OrderFailureException("Cannot update due to order status " +
                    orderEntity.getStatus() +
                    ".Can only update while waiting");

        Map<Long, OrderItemUpdateRequest> nonNullRequests = orderItemUpdateRequests.stream()
                .filter(orderItemUpdateRequest -> orderItemUpdateRequest.id() != null)
                .collect(Collectors.toMap(OrderItemUpdateRequest::id, orderItemUpdateRequest -> orderItemUpdateRequest));

        orderEntity.getOrderItemEntityList().removeIf(
                orderItemEntity -> !nonNullRequests.containsKey(orderItemEntity.getId()));

        Map<Long, OrderItemEntity> existingItems = orderEntity.getOrderItemEntityList()
                .stream()
                .collect(Collectors.toMap(OrderItemEntity::getId, orderItemEntity -> orderItemEntity));


        List<Long> menuIds = orderItemUpdateRequests.stream().map(OrderItemUpdateRequest::menuId).toList();
        Map<Long, MenuEntity> menuEntityMap = menuService.getMenuEntityMapByIds(menuIds);


        AtomicInteger orderTotalPrice = new AtomicInteger();

        for (OrderItemUpdateRequest orderItemUpdateRequest : orderItemUpdateRequests) {

            MenuEntity menuEntity = menuEntityMap.get(orderItemUpdateRequest.menuId());

            int unitPrice = menuEntity.getCurrentPrice();
            int quantity = orderItemUpdateRequest.quantity();
            int totalPrice = unitPrice * quantity;
            orderTotalPrice.addAndGet(totalPrice);

            if (existingItems.containsKey(orderItemUpdateRequest.id())) {
                //update the existing item in list this will direct update the hibernate object
                OrderItemEntity orderItemEntity = existingItems.get(orderItemUpdateRequest.id());
                orderItemEntity.setTotalPrice(totalPrice);
                orderItemEntity.setQuantity(quantity);
                orderItemEntity.setUnitPrice(unitPrice);
                orderItemEntity.setMenuEntity(menuEntity);

            } else {
                OrderItemEntity orderItemEntity = new OrderItemEntity();
                orderItemEntity.setMenuEntity(menuEntity);
                orderItemEntity.setOrderEntity(orderEntity);
                orderItemEntity.setQuantity(quantity);
                orderItemEntity.setTotalPrice(totalPrice);
                orderItemEntity.setUnitPrice(unitPrice);
                orderEntity.getOrderItemEntityList().add(orderItemEntity);

            }
        }
        orderEntity.setTotalPrice(orderTotalPrice.get());
        OrderEntity savedOrder = orderRepo.save(orderEntity);
        //map orderItemEntity to orderItemResponse to get the db generated id
        List<OrderItemResponse> orderItemResponses = savedOrder.getOrderItemEntityList()
                .stream()
                .map(
                        orderItemEntity -> new OrderItemResponse(
                                orderItemEntity.getId(),
                                orderItemEntity.getMenuEntity().getId(),
                                orderItemEntity.getMenuEntity().getName(),
                                orderItemEntity.getQuantity(),
                                orderItemEntity.getUnitPrice(),
                                orderItemEntity.getTotalPrice()
                        )
                ).toList();

        return new OrderResponse(
                savedOrder.getId(),
                userId,
                savedOrder.getStatus(),
                "order updated successfully",
                orderItemResponses, savedOrder.getTotalPrice());


    }


}
