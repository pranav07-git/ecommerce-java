package com.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SellerReport(BigDecimal revenue, int unitsSold, int orderCount,
                           Map<LocalDate, BigDecimal> revenueByDay, Map<String, Integer> topProducts) {
}
