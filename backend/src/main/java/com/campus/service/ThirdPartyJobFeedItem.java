package com.campus.service;

public record ThirdPartyJobFeedItem(
        String title,
        String companyName,
        String city,
        String jobType,
        String educationRequirement,
        String sourceUrl,
        String summary,
        String content,
        String deadlineAt) {
}
