package com.commodule.template.catalog;

import com.commodule.web.ApiException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CatalogService {

    private final Map<Long, String> productNames = Map.of(
            1L, "Mechanical Keyboard",
            2L, "USB-C Dock"
    );
    private final AtomicInteger loads = new AtomicInteger();

    @Cacheable(cacheNames = "products", key = "#id")
    public Product getProduct(long id) {
        String name = productNames.get(id);
        if (name == null) {
            throw ApiException.notFound("商品不存在: " + id);
        }
        return new Product(id, name, loads.incrementAndGet());
    }
}

