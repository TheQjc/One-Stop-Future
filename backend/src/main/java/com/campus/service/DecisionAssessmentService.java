package com.campus.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.dto.DecisionAssessmentQuestionResponse;
import com.campus.dto.DecisionAssessmentQuestionResponse.OptionItem;
import com.campus.dto.DecisionAssessmentQuestionResponse.QuestionItem;
import com.campus.dto.DecisionAssessmentResultResponse;
import com.campus.dto.DecisionAssessmentResultResponse.NextActionItem;
import com.campus.dto.DecisionAssessmentResultResponse.RankItem;
import com.campus.dto.DecisionAssessmentResultResponse.ScoreBundle;
import com.campus.dto.DecisionAssessmentSubmissionRequest;
import com.campus.dto.DecisionAssessmentSubmissionRequest.AnswerItem;
import com.campus.entity.DecisionAssessmentOption;
import com.campus.entity.DecisionAssessmentQuestion;
import com.campus.entity.DecisionAssessmentSession;
import com.campus.mapper.DecisionAssessmentOptionMapper;
import com.campus.mapper.DecisionAssessmentQuestionMapper;
import com.campus.mapper.DecisionAssessmentSessionMapper;

@Service
public class DecisionAssessmentService {

    private static final int ACTIVE = 1;
    private static final List<String> TRACK_TIEBREAK_ORDER = List.of("EXAM", "CAREER", "ABROAD");
    private static final List<NextActionItem> DEFAULT_NEXT_ACTIONS = List.of(
            new NextActionItem("TIMELINE", "Go to Timeline", "/timeline"),
            new NextActionItem("COMPARE_SCHOOLS", "Compare Schools", "/schools/compare"));

    private final DecisionAssessmentQuestionMapper questionMapper;
    private final DecisionAssessmentOptionMapper optionMapper;
    private final DecisionAssessmentSessionMapper sessionMapper;

    public DecisionAssessmentService(DecisionAssessmentQuestionMapper questionMapper,
            DecisionAssessmentOptionMapper optionMapper,
            DecisionAssessmentSessionMapper sessionMapper) {
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
        this.sessionMapper = sessionMapper;
    }

    public DecisionAssessmentQuestionResponse listQuestions() {
        List<DecisionAssessmentQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<DecisionAssessmentQuestion>()
                .eq(DecisionAssessmentQuestion::getIsActive, ACTIVE)
                .orderByAsc(DecisionAssessmentQuestion::getDisplayOrder)
                .orderByAsc(DecisionAssessmentQuestion::getId));

        if (questions.isEmpty()) {
            return new DecisionAssessmentQuestionResponse(List.of());
        }

        List<Long> questionIds = questions.stream()
                .map(DecisionAssessmentQuestion::getId)
                .toList();

        List<DecisionAssessmentOption> options = optionMapper.selectList(new LambdaQueryWrapper<DecisionAssessmentOption>()
                .eq(DecisionAssessmentOption::getIsActive, ACTIVE)
                .in(DecisionAssessmentOption::getQuestionId, questionIds)
                .orderByAsc(DecisionAssessmentOption::getQuestionId)
                .orderByAsc(DecisionAssessmentOption::getDisplayOrder)
                .orderByAsc(DecisionAssessmentOption::getId));

        Map<Long, List<OptionItem>> optionItemsByQuestionId = new LinkedHashMap<>();
        for (DecisionAssessmentOption option : options) {
            optionItemsByQuestionId.computeIfAbsent(option.getQuestionId(), ignored -> new ArrayList<>())
                    .add(toOptionItem(option));
        }

        List<QuestionItem> questionItems = questions.stream()
                .map(question -> toQuestionItem(question,
                        optionItemsByQuestionId.getOrDefault(question.getId(), List.of())))
                .toList();

        return new DecisionAssessmentQuestionResponse(questionItems);
    }

    public DecisionAssessmentResultResponse submit(String identity, DecisionAssessmentSubmissionRequest request) {
        Long userId = requireUserId(identity);
        if (request == null || request.answers() == null) {
            throw new BusinessException(400, "invalid request");
        }

        List<DecisionAssessmentQuestion> activeQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<DecisionAssessmentQuestion>()
                        .eq(DecisionAssessmentQuestion::getIsActive, ACTIVE)
                        .orderByAsc(DecisionAssessmentQuestion::getDisplayOrder)
                        .orderByAsc(DecisionAssessmentQuestion::getId));
        if (activeQuestions.isEmpty()) {
            throw new BusinessException(400, "assessment questions not configured");
        }

        Set<Long> requiredQuestionIds = activeQuestions.stream()
                .map(DecisionAssessmentQuestion::getId)
                .collect(java.util.stream.Collectors.toSet());

        List<AnswerItem> answers = request.answers();
        if (answers.size() != requiredQuestionIds.size()) {
            throw new BusinessException(400, "incomplete answers");
        }

        Map<Long, Long> optionIdByQuestionId = new LinkedHashMap<>();
        for (AnswerItem answer : answers) {
            if (answer == null || answer.questionId() == null || answer.optionId() == null) {
                throw new BusinessException(400, "invalid request");
            }
            Long existing = optionIdByQuestionId.putIfAbsent(answer.questionId(), answer.optionId());
            if (existing != null) {
                throw new BusinessException(400, "duplicate question answers");
            }
        }

        if (!optionIdByQuestionId.keySet().containsAll(requiredQuestionIds)
                || !requiredQuestionIds.containsAll(optionIdByQuestionId.keySet())) {
            throw new BusinessException(400, "unknown question");
        }

        List<Long> optionIds = optionIdByQuestionId.values().stream().toList();
        List<DecisionAssessmentOption> selectedOptions = optionMapper.selectList(new LambdaQueryWrapper<DecisionAssessmentOption>()
                .eq(DecisionAssessmentOption::getIsActive, ACTIVE)
                .in(DecisionAssessmentOption::getId, optionIds));
        if (selectedOptions.size() != optionIds.size()) {
            throw new BusinessException(400, "unknown option");
        }

        Map<Long, DecisionAssessmentOption> optionById = new LinkedHashMap<>();
        for (DecisionAssessmentOption option : selectedOptions) {
            optionById.put(option.getId(), option);
        }

        int careerScore = 0;
        int examScore = 0;
        int abroadScore = 0;
        for (Map.Entry<Long, Long> entry : optionIdByQuestionId.entrySet()) {
            Long questionId = entry.getKey();
            DecisionAssessmentOption option = optionById.get(entry.getValue());
            if (option == null || option.getQuestionId() == null || !option.getQuestionId().equals(questionId)) {
                throw new BusinessException(400, "option does not belong to question");
            }
            careerScore += safeInt(option.getCareerScore());
            examScore += safeInt(option.getExamScore());
            abroadScore += safeInt(option.getAbroadScore());
        }

        ScoreBundle scores = new ScoreBundle(careerScore, examScore, abroadScore);
        List<RankItem> ranking = rankingFor(scores);
        String recommendedTrack = ranking.isEmpty() ? null : ranking.get(0).track();
        if (recommendedTrack == null) {
            throw new BusinessException(400, "invalid score result");
        }

        String summaryText = summaryTextFor(recommendedTrack);
        LocalDate sessionDate = LocalDate.now();

        DecisionAssessmentSession session = new DecisionAssessmentSession();
        session.setUserId(userId);
        session.setRecommendedTrack(recommendedTrack);
        session.setCareerScore(careerScore);
        session.setExamScore(examScore);
        session.setAbroadScore(abroadScore);
        session.setSummaryText(summaryText);
        session.setSessionDate(sessionDate);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        return new DecisionAssessmentResultResponse(true, recommendedTrack, summaryText, scores, ranking, sessionDate,
                DEFAULT_NEXT_ACTIONS);
    }

    public DecisionAssessmentResultResponse latestFor(String identity) {
        Long userId = requireUserId(identity);
        DecisionAssessmentSession latest = sessionMapper.selectOne(new LambdaQueryWrapper<DecisionAssessmentSession>()
                .eq(DecisionAssessmentSession::getUserId, userId)
                .orderByDesc(DecisionAssessmentSession::getCreatedAt)
                .orderByDesc(DecisionAssessmentSession::getId)
                .last("LIMIT 1"));

        if (latest == null) {
            return new DecisionAssessmentResultResponse(false, null, null, null, List.of(), null, List.of());
        }

        ScoreBundle scores = new ScoreBundle(
                safeInt(latest.getCareerScore()),
                safeInt(latest.getExamScore()),
                safeInt(latest.getAbroadScore()));
        List<RankItem> ranking = rankingFor(scores);

        return new DecisionAssessmentResultResponse(true, latest.getRecommendedTrack(), latest.getSummaryText(), scores,
                ranking, latest.getSessionDate(), DEFAULT_NEXT_ACTIONS);
    }

    private QuestionItem toQuestionItem(DecisionAssessmentQuestion question, List<OptionItem> options) {
        return new QuestionItem(
                question.getId(),
                question.getCode(),
                question.getPrompt(),
                question.getDescription(),
                safeInt(question.getDisplayOrder()),
                options);
    }

    private OptionItem toOptionItem(DecisionAssessmentOption option) {
        return new OptionItem(
                option.getId(),
                option.getCode(),
                option.getLabel(),
                option.getDescription(),
                safeInt(option.getDisplayOrder()));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
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

    private List<RankItem> rankingFor(ScoreBundle scores) {
        List<RankItem> items = new ArrayList<>(3);
        items.add(new RankItem("CAREER", scores.career()));
        items.add(new RankItem("EXAM", scores.exam()));
        items.add(new RankItem("ABROAD", scores.abroad()));
        items.sort(Comparator.<RankItem>comparingInt(RankItem::score).reversed()
                .thenComparingInt(item -> TRACK_TIEBREAK_ORDER.indexOf(item.track())));
        return items;
    }

    private String summaryTextFor(String recommendedTrack) {
        return switch (recommendedTrack) {
            case "EXAM" -> "Recommendation: EXAM. Focus on structured study goals and measurable score gains.";
            case "CAREER" -> "Recommendation: CAREER. Focus on projects, interview readiness, and employability wins.";
            case "ABROAD" -> "Recommendation: ABROAD. Focus on language tests, application milestones, and timelines.";
            default -> "Recommendation ready. Focus on the next steps to move forward.";
        };
    }
}
