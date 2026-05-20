package com.goylik.order_service.repository;

import com.goylik.order_service.model.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    boolean existsByName(String name);
}