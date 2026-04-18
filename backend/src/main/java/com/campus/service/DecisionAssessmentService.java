package com.campus.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dto.DecisionAssessmentQuestionResponse;
import com.campus.dto.DecisionAssessmentQuestionResponse.OptionItem;
import com.campus.dto.DecisionAssessmentQuestionResponse.QuestionItem;
import com.campus.entity.DecisionAssessmentOption;
import com.campus.entity.DecisionAssessmentQuestion;
import com.campus.mapper.DecisionAssessmentOptionMapper;
import com.campus.mapper.DecisionAssessmentQuestionMapper;

@Service
public class DecisionAssessmentService {

    private static final int ACTIVE = 1;

    private final DecisionAssessmentQuestionMapper questionMapper;
    private final DecisionAssessmentOptionMapper optionMapper;

    public DecisionAssessmentService(DecisionAssessmentQuestionMapper questionMapper,
            DecisionAssessmentOptionMapper optionMapper) {
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
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
}

