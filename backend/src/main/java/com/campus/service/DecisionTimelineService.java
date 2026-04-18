package com.campus.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.dto.DecisionTimelineResponse;
import com.campus.dto.DecisionTimelineResponse.TimelineItem;
import com.campus.entity.DecisionAssessmentSession;
import com.campus.entity.DecisionTimelineMilestone;
import com.campus.mapper.DecisionAssessmentSessionMapper;
import com.campus.mapper.DecisionTimelineMilestoneMapper;

@Service
public class DecisionTimelineService {

    private static final int ACTIVE = 1;

    private final DecisionTimelineMilestoneMapper milestoneMapper;
    private final DecisionAssessmentSessionMapper sessionMapper;

    public DecisionTimelineService(DecisionTimelineMilestoneMapper milestoneMapper,
            DecisionAssessmentSessionMapper sessionMapper) {
        this.milestoneMapper = milestoneMapper;
        this.sessionMapper = sessionMapper;
    }

    public DecisionTimelineResponse timelineFor(String identity, String track, LocalDate anchorDate) {
        String normalizedTrack = normalizeTrack(track);
        LocalDate resolvedAnchorDate = anchorDate != null ? anchorDate : latestSessionDateFor(identity);
        if (resolvedAnchorDate == null) {
            return new DecisionTimelineResponse(normalizedTrack, null, true, List.of());
        }

        List<DecisionTimelineMilestone> milestones = milestoneMapper.selectList(new LambdaQueryWrapper<DecisionTimelineMilestone>()
                .eq(DecisionTimelineMilestone::getIsActive, ACTIVE)
                .eq(DecisionTimelineMilestone::getTrack, normalizedTrack)
                .orderByAsc(DecisionTimelineMilestone::getDisplayOrder)
                .orderByAsc(DecisionTimelineMilestone::getId));

        List<TimelineItem> items = milestones.stream()
                .map(milestone -> toItem(milestone, resolvedAnchorDate))
                .toList();

        return new DecisionTimelineResponse(normalizedTrack, resolvedAnchorDate, false, items);
    }

    private TimelineItem toItem(DecisionTimelineMilestone milestone, LocalDate anchorDate) {
        int offsetMonths = safeInt(milestone.getOffsetMonths());
        int offsetDays = safeInt(milestone.getOffsetDays());
        LocalDate targetDate = anchorDate.plusMonths(offsetMonths).plusDays(offsetDays);
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);

        return new TimelineItem(
                milestone.getPhaseCode(),
                milestone.getPhaseLabel(),
                milestone.getTitle(),
                milestone.getSummary(),
                targetDate,
                remainingDays,
                parseChecklist(milestone.getActionChecklist()),
                milestone.getResourceHint());
    }

    private List<String> parseChecklist(String checklist) {
        if (checklist == null || checklist.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(checklist.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private LocalDate latestSessionDateFor(String identity) {
        Long userId = requireUserId(identity);
        DecisionAssessmentSession latest = sessionMapper.selectOne(new LambdaQueryWrapper<DecisionAssessmentSession>()
                .eq(DecisionAssessmentSession::getUserId, userId)
                .orderByDesc(DecisionAssessmentSession::getCreatedAt)
                .orderByDesc(DecisionAssessmentSession::getId)
                .last("LIMIT 1"));
        if (latest == null) {
            return null;
        }
        return latest.getSessionDate();
    }

    private Long requireUserId(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new BusinessException(401, "unauthorized");
        }
        if (!identity.matches("^\\d+$")) {
            throw new BusinessException(401, "unauthorized");
        }
        try {
            return Long.parseLong(identity);
        } catch (NumberFormatException ex) {
            throw new BusinessException(401, "unauthorized");
        }
    }

    private String normalizeTrack(String track) {
        if (track == null || track.isBlank()) {
            throw new BusinessException(400, "track is required");
        }
        String normalized = track.trim().toUpperCase(Locale.ROOT);
        if (!"CAREER".equals(normalized) && !"EXAM".equals(normalized) && !"ABROAD".equals(normalized)) {
            throw new BusinessException(400, "invalid track");
        }
        return normalized;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

