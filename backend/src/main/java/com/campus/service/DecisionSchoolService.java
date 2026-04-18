package com.campus.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.dto.DecisionSchoolCompareRequest;
import com.campus.dto.DecisionSchoolCompareResponse;
import com.campus.dto.DecisionSchoolCompareResponse.ChartPointItem;
import com.campus.dto.DecisionSchoolCompareResponse.ChartSeriesItem;
import com.campus.dto.DecisionSchoolCompareResponse.MetricDefinitionItem;
import com.campus.dto.DecisionSchoolCompareResponse.TableCellItem;
import com.campus.dto.DecisionSchoolCompareResponse.TableRowItem;
import com.campus.dto.DecisionSchoolListResponse;
import com.campus.dto.DecisionSchoolListResponse.SchoolItem;
import com.campus.entity.DecisionSchoolMetric;
import com.campus.entity.DecisionSchoolMetricDefinition;
import com.campus.entity.DecisionSchoolProfile;
import com.campus.mapper.DecisionSchoolMetricDefinitionMapper;
import com.campus.mapper.DecisionSchoolMetricMapper;
import com.campus.mapper.DecisionSchoolProfileMapper;

@Service
public class DecisionSchoolService {

    private static final int ACTIVE = 1;
    private static final int MIN_COMPARE = 2;
    private static final int MAX_COMPARE = 4;

    private final DecisionSchoolProfileMapper profileMapper;
    private final DecisionSchoolMetricDefinitionMapper definitionMapper;
    private final DecisionSchoolMetricMapper metricMapper;

    public DecisionSchoolService(DecisionSchoolProfileMapper profileMapper,
            DecisionSchoolMetricDefinitionMapper definitionMapper,
            DecisionSchoolMetricMapper metricMapper) {
        this.profileMapper = profileMapper;
        this.definitionMapper = definitionMapper;
        this.metricMapper = metricMapper;
    }

    public DecisionSchoolListResponse listSchools(String track, String keyword) {
        String normalizedTrack = normalizeTrack(track);
        String normalizedKeyword = normalizeOptional(keyword);

        LambdaQueryWrapper<DecisionSchoolProfile> wrapper = new LambdaQueryWrapper<DecisionSchoolProfile>()
                .eq(DecisionSchoolProfile::getIsActive, ACTIVE)
                .eq(DecisionSchoolProfile::getTrack, normalizedTrack)
                .orderByAsc(DecisionSchoolProfile::getId);

        if (normalizedKeyword != null) {
            wrapper.and(q -> q.like(DecisionSchoolProfile::getName, normalizedKeyword)
                    .or()
                    .like(DecisionSchoolProfile::getRegion, normalizedKeyword));
        }

        List<SchoolItem> schools = profileMapper.selectList(wrapper).stream()
                .map(this::toSchoolItem)
                .toList();

        return new DecisionSchoolListResponse(normalizedTrack, normalizedKeyword, schools.size(), schools);
    }

    public DecisionSchoolCompareResponse compare(DecisionSchoolCompareRequest request) {
        if (request == null || request.schoolIds() == null) {
            throw new BusinessException(400, "invalid request");
        }

        List<Long> schoolIds = request.schoolIds();
        if (schoolIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(400, "invalid request");
        }

        if (schoolIds.size() < MIN_COMPARE) {
            throw new BusinessException(400, "at least 2 schools required");
        }
        if (schoolIds.size() > MAX_COMPARE) {
            throw new BusinessException(400, "at most 4 schools allowed");
        }

        Set<Long> deduped = Set.copyOf(schoolIds);
        if (deduped.size() != schoolIds.size()) {
            throw new BusinessException(400, "duplicate school ids");
        }

        List<DecisionSchoolProfile> profiles = profileMapper.selectList(new LambdaQueryWrapper<DecisionSchoolProfile>()
                .eq(DecisionSchoolProfile::getIsActive, ACTIVE)
                .in(DecisionSchoolProfile::getId, schoolIds));
        if (profiles.size() != schoolIds.size()) {
            throw new BusinessException(400, "school not found");
        }

        Map<Long, DecisionSchoolProfile> profileById = new LinkedHashMap<>();
        for (DecisionSchoolProfile profile : profiles) {
            profileById.put(profile.getId(), profile);
        }

        String track = requireSameSupportedTrack(schoolIds, profileById);

        List<DecisionSchoolMetricDefinition> definitions = definitionMapper.selectList(
                new LambdaQueryWrapper<DecisionSchoolMetricDefinition>()
                        .eq(DecisionSchoolMetricDefinition::getIsActive, ACTIVE)
                        .eq(DecisionSchoolMetricDefinition::getTrack, track)
                        .orderByAsc(DecisionSchoolMetricDefinition::getMetricOrder)
                        .orderByAsc(DecisionSchoolMetricDefinition::getId));

        List<MetricDefinitionItem> definitionItems = definitions.stream()
                .map(this::toMetricDefinitionItem)
                .toList();

        Map<Long, Map<String, String>> metricsBySchoolId = metricsBySchoolId(schoolIds);

        List<SchoolItem> schools = schoolIds.stream()
                .map(id -> toSchoolItem(profileById.get(id)))
                .toList();

        List<TableRowItem> tableRows = new ArrayList<>();
        List<ChartSeriesItem> chartSeries = new ArrayList<>();

        for (DecisionSchoolMetricDefinition def : definitions) {
            List<TableCellItem> cells = new ArrayList<>();
            for (Long schoolId : schoolIds) {
                String raw = metricsBySchoolId.getOrDefault(schoolId, Map.of()).get(def.getMetricCode());
                cells.add(toTableCell(schoolId, raw));
            }
            tableRows.add(new TableRowItem(def.getMetricCode(), def.getMetricLabel(), def.getMetricUnit(),
                    def.getValueType(), cells));

            if (isChartable(def)) {
                List<ChartPointItem> points = new ArrayList<>();
                for (Long schoolId : schoolIds) {
                    DecisionSchoolProfile profile = profileById.get(schoolId);
                    String raw = metricsBySchoolId.getOrDefault(schoolId, Map.of()).get(def.getMetricCode());
                    points.add(toChartPoint(schoolId, profile != null ? profile.getName() : null, raw));
                }
                chartSeries.add(new ChartSeriesItem(def.getMetricCode(), def.getMetricLabel(), def.getMetricUnit(),
                        def.getValueType(), points));
            }
        }

        String highlightSummary = "Comparison ready. Review the table and charts to pick trade-offs.";
        return new DecisionSchoolCompareResponse(schools, definitionItems, tableRows, chartSeries, highlightSummary);
    }

    private Map<Long, Map<String, String>> metricsBySchoolId(List<Long> schoolIds) {
        List<DecisionSchoolMetric> metrics = metricMapper.selectList(new LambdaQueryWrapper<DecisionSchoolMetric>()
                .in(DecisionSchoolMetric::getSchoolId, schoolIds)
                .orderByAsc(DecisionSchoolMetric::getSchoolId)
                .orderByAsc(DecisionSchoolMetric::getMetricCode)
                .orderByAsc(DecisionSchoolMetric::getId));

        Map<Long, Map<String, String>> bySchool = new LinkedHashMap<>();
        for (DecisionSchoolMetric metric : metrics) {
            bySchool.computeIfAbsent(metric.getSchoolId(), ignored -> new LinkedHashMap<>())
                    .put(metric.getMetricCode(), metric.getMetricValue());
        }
        return bySchool;
    }

    private String requireSameSupportedTrack(List<Long> requestedIds, Map<Long, DecisionSchoolProfile> profileById) {
        String track = null;
        for (Long id : requestedIds) {
            DecisionSchoolProfile profile = profileById.get(id);
            if (profile == null) {
                throw new BusinessException(400, "school not found");
            }
            if (track == null) {
                track = profile.getTrack();
            } else if (!track.equals(profile.getTrack())) {
                throw new BusinessException(400, "mixed school tracks");
            }
        }
        return normalizeTrack(track);
    }

    private SchoolItem toSchoolItem(DecisionSchoolProfile profile) {
        return new SchoolItem(profile.getId(), profile.getName(), profile.getTrack(), profile.getRegion(),
                profile.getTierLabel());
    }

    private MetricDefinitionItem toMetricDefinitionItem(DecisionSchoolMetricDefinition def) {
        return new MetricDefinitionItem(def.getMetricCode(), def.getMetricLabel(), def.getMetricUnit(), def.getValueType(),
                isChartable(def), safeInt(def.getMetricOrder()));
    }

    private TableCellItem toTableCell(Long schoolId, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new TableCellItem(schoolId, "N/A", null, true);
        }
        return new TableCellItem(schoolId, rawValue, rawValue, false);
    }

    private ChartPointItem toChartPoint(Long schoolId, String schoolName, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new ChartPointItem(schoolId, schoolName, null, "N/A", true);
        }
        Double numeric = tryParseDouble(rawValue);
        if (numeric == null) {
            return new ChartPointItem(schoolId, schoolName, null, rawValue, true);
        }
        return new ChartPointItem(schoolId, schoolName, numeric, rawValue, false);
    }

    private Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isChartable(DecisionSchoolMetricDefinition def) {
        return def.getChartable() != null && def.getChartable() == 1;
    }

    private String normalizeTrack(String track) {
        if (track == null || track.isBlank()) {
            throw new BusinessException(400, "track is required");
        }
        String normalized = track.trim().toUpperCase(Locale.ROOT);
        if (!"EXAM".equals(normalized) && !"ABROAD".equals(normalized)) {
            throw new BusinessException(400, "invalid track");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
