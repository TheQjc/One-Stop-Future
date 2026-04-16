package com.campus.dto;

import java.util.List;

public record DiscoverResponse(
        String tab,
        String period,
        int total,
        List<DiscoverItemView> items) {
}
