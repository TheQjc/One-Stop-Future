package com.campus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.JobImportValidationException;
import com.campus.common.JobPostingStatus;
import com.campus.dto.AdminJobImportResponse;
import com.campus.dto.AdminJobImportValidationError;
import com.campus.dto.AdminJobImportValidationResponse;
import com.campus.entity.JobPosting;
import com.campus.entity.User;
import com.campus.mapper.JobPostingMapper;

@Service
public class JobBatchImportService {

    private final JobPostingMapper jobPostingMapper;
    private final UserService userService;
    private final JobImportCsvParser parser;
    private final JobPostingDraftFactory draftFactory;

    public JobBatchImportService(
            JobPostingMapper jobPostingMapper,
            UserService userService,
            JobImportCsvParser parser,
            JobPostingDraftFactory draftFactory) {
        this.jobPostingMapper = jobPostingMapper;
        this.userService = userService;
        this.parser = parser;
        this.draftFactory = draftFactory;
    }

    @Transactional
    public AdminJobImportResponse importJobs(String identity, MultipartFile file) {
        User admin = userService.requireByIdentity(identity);
        String fileName = displayFileName(file);
        List<JobImportRow> rows = parser.parse(file);
        LocalDateTime now = LocalDateTime.now();

        List<JobPosting> drafts = new ArrayList<>();
        List<AdminJobImportValidationError> errors = new ArrayList<>();
        Map<String, Integer> rowBySourceUrl = new HashMap<>();

        for (JobImportRow row : rows) {
            JobPostingDraftFactory.BuildResult result = draftFactory.build(row, admin.getId(), now);
            errors.addAll(result.errors());
            if (!result.isValid()) {
                continue;
            }

            JobPosting draft = result.job();
            Integer existingRow = rowBySourceUrl.putIfAbsent(draft.getSourceUrl(), row.rowNumber());
            if (existingRow != null) {
                errors.add(new AdminJobImportValidationError(row.rowNumber(), "sourceUrl", "duplicate source url in file"));
                continue;
            }
            drafts.add(draft);
        }

        errors.addAll(findExistingSourceUrlConflicts(drafts, rowBySourceUrl));
        if (!errors.isEmpty()) {
            throw new JobImportValidationException(new AdminJobImportValidationResponse(
                    fileName,
                    rows.size(),
                    0,
                    sortErrors(errors)));
        }

        drafts.forEach(jobPostingMapper::insert);
        return new AdminJobImportResponse(fileName, rows.size(), drafts.size(), JobPostingStatus.DRAFT.name());
    }

    private List<AdminJobImportValidationError> findExistingSourceUrlConflicts(
            List<JobPosting> drafts,
            Map<String, Integer> rowBySourceUrl) {
        if (drafts.isEmpty()) {
            return List.of();
        }

        List<String> sourceUrls = drafts.stream()
                .map(JobPosting::getSourceUrl)
                .distinct()
                .toList();

        List<JobPosting> conflicts = jobPostingMapper.selectList(new LambdaQueryWrapper<JobPosting>()
                .in(JobPosting::getSourceUrl, sourceUrls)
                .ne(JobPosting::getStatus, JobPostingStatus.DELETED.name()));

        if (conflicts.isEmpty()) {
            return List.of();
        }

        List<AdminJobImportValidationError> errors = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        for (JobPosting conflict : conflicts) {
            String sourceUrl = conflict.getSourceUrl();
            if (!seenUrls.add(sourceUrl)) {
                continue;
            }
            Integer rowNumber = rowBySourceUrl.get(sourceUrl);
            if (rowNumber != null) {
                errors.add(new AdminJobImportValidationError(rowNumber, "sourceUrl", "duplicate source url already exists"));
            }
        }
        return errors;
    }

    private List<AdminJobImportValidationError> sortErrors(List<AdminJobImportValidationError> errors) {
        return errors.stream()
                .sorted(Comparator.comparingInt(AdminJobImportValidationError::rowNumber)
                        .thenComparing(AdminJobImportValidationError::column))
                .toList();
    }

    private String displayFileName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            return "";
        }
        return file.getOriginalFilename().trim();
    }
}
