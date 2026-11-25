package com.commodule.template.order;

import com.commodule.datapermission.DataPage;
import com.commodule.datapermission.DataPermission;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final List<Order> orders = List.of(
            new Order(101L, 1L, "admin keyboard"),
            new Order(102L, 1L, "admin dock"),
            new Order(201L, 2L, "user mouse")
    );

    @DataPermission(resource = "order", ownerField = "ownerId")
    public List<Order> listOrders() {
        return orders;
    }

    @DataPermission(resource = "order", ownerField = "ownerId")
    public DataPage<Order> pageOrders() {
        return new DataPage<>(orders, orders.size());
    }
}

