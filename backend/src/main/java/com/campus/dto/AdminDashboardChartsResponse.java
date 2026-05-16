package com.campus.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class AdminDashboardChartsResponse {
    private List<TrendData> registrationTrends;
    private List<TrendData> postTrends;
    private List<TrendData> activeUserTrends;
    private List<TagData> tagProportions;
    private List<RankingData> downloadRankings;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrendData {
        private String date;
        private Integer count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TagData {
        private String tag;
        private Integer count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RankingData {
        private String title;
        private Integer count;
    }
}
