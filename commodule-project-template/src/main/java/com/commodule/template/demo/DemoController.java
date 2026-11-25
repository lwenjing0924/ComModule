package com.commodule.template.demo;

import com.commodule.auth.AuthPrincipal;
import com.commodule.mq.Message;
import com.commodule.mq.MessageBroker;
import com.commodule.template.catalog.CatalogService;
import com.commodule.template.catalog.Product;
import com.commodule.template.messaging.DemoOrderListener;
import com.commodule.datapermission.DataPage;
import com.commodule.template.order.Order;
import com.commodule.template.order.OrderService;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Validated
public class DemoController {

    private final CatalogService catalogService;
    private final MessageBroker messageBroker;
    private final DemoOrderListener orderListener;
    private final OrderService orderService;

    public DemoController(
            CatalogService catalogService,
            MessageBroker messageBroker,
            DemoOrderListener orderListener,
            OrderService orderService
    ) {
        this.catalogService = catalogService;
        this.messageBroker = messageBroker;
        this.orderListener = orderListener;
        this.orderService = orderService;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return Map.of(
                "id", principal.id(),
                "username", principal.username(),
                "roles", principal.roles()
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminOnly() {
        return Map.of("message", "admin access granted");
    }

    @GetMapping("/cache/{id}")
    public Product cachedProduct(@PathVariable long id) {
        return catalogService.getProduct(id);
    }

    @PostMapping("/events/{eventName}")
    public Map<String, String> publishEvent(@PathVariable String eventName) {
        String messageId = messageBroker.publish("demo.orders", eventName);
        return Map.of("messageId", messageId);
    }

    @GetMapping("/events")
    public List<Message> receivedEvents() {
        return orderListener.received();
    }

    @GetMapping("/orders")
    public List<Order> orders() {
        return orderService.listOrders();
    }

    @GetMapping("/orders/page")
    public DataPage<Order> orderPage() {
        return orderService.pageOrders();
    }

    @GetMapping("/echo/{count}")
    public Map<String, Integer> echo(@PathVariable @Min(1) int count) {
        return Map.of("count", count);
    }
}

