package com.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

public record SellerReport(BigDecimal revenue, int unitsSold, int orderCount,
                           List<String> days, List<BigDecimal> revenueByDay,
                           List<String> topNames, List<Integer> topUnits) {
}
