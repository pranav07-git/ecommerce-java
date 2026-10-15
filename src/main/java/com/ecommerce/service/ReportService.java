package com.ecommerce.service;

import com.ecommerce.model.OrderItem;
import com.ecommerce.model.User;
import com.ecommerce.repo.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final OrderItemRepository items;

    public ReportService(OrderItemRepository items) {
        this.items = items;
    }

    public SellerReport sellerReport(User seller) {
        List<OrderItem> sold = items.findSoldBy(seller);
        BigDecimal revenue = sold.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int units = sold.stream().mapToInt(OrderItem::getQuantity).sum();
        int orders = (int) sold.stream().map(i -> i.getOrder().getId()).distinct().count();

        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        for (OrderItem i : sold) {
            byDay.merge(i.getOrder().getCreatedAt().toLocalDate(), i.getSubtotal(), BigDecimal::add);
        }
        Map<String, Integer> byProduct = sold.stream().collect(
                Collectors.groupingBy(i -> i.getProduct().getName(), Collectors.summingInt(OrderItem::getQuantity)));
        List<Map.Entry<String, Integer>> top = byProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5).toList();

        return new SellerReport(revenue, units, orders,
                byDay.keySet().stream().map(LocalDate::toString).toList(),
                new ArrayList<>(byDay.values()),
                top.stream().map(Map.Entry::getKey).toList(),
                top.stream().map(Map.Entry::getValue).toList());
    }
}
