package com.campus.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.campus.common.BusinessException;
import com.campus.common.JobImportValidationException;
import com.campus.dto.AdminJobImportValidationError;
import com.campus.dto.AdminJobImportValidationResponse;

@Service
public class JobImportCsvParser {

    private static final int MAX_DATA_ROWS = 200;
    private static final String FILE_COLUMN = "file";
    private static final String MESSAGE_FILE_REQUIRED = "file is required";
    private static final String MESSAGE_FILE_EMPTY = "csv file is empty";
    private static final String MESSAGE_FILE_TYPE = "job import only supports csv files";
    private static final String MESSAGE_FILE_NAME = "csv file name is required";
    private static final String MESSAGE_FILE_UTF8 = "csv file must be utf-8 encoded";
    private static final String MESSAGE_ROW_LIMIT = "job import row limit exceeded";
    private static final String MESSAGE_MISSING_HEADER = "missing required header";
    private static final String MESSAGE_DUPLICATE_HEADER = "duplicate header";
    private static final String MESSAGE_UNSUPPORTED_HEADER = "unsupported header";

    private static final List<String> REQUIRED_HEADERS = List.of(
            "title",
            "companyName",
            "city",
            "jobType",
            "educationRequirement",
            "sourcePlatform",
            "sourceUrl",
            "summary");

    private static final List<String> OPTIONAL_HEADERS = List.of("content", "deadlineAt");

    private static final Set<String> APPROVED_HEADERS = new LinkedHashSet<>();

    static {
        APPROVED_HEADERS.addAll(REQUIRED_HEADERS);
        APPROVED_HEADERS.addAll(OPTIONAL_HEADERS);
    }

    public List<JobImportRow> parse(MultipartFile file) {
        String fileName = displayFileName(file);
        validateFileBoundary(fileName, file);

        try (Reader reader = utf8Reader(file);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(false)
                        .build()
                        .parse(reader)) {
            List<String> headerNames = normalizeHeaderNames(parser.getHeaderNames());
            validateHeaders(fileName, headerNames);

            List<CSVRecord> records = parser.getRecords();
            validateRowCount(fileName, records.size());

            Set<String> availableHeaders = new LinkedHashSet<>(headerNames);
            List<JobImportRow> rows = new ArrayList<>(records.size());
            for (int index = 0; index < records.size(); index++) {
                rows.add(toRow(index + 2, records.get(index), availableHeaders));
            }
            return rows;
        } catch (CharacterCodingException exception) {
            throw validationFailure(fileName, 0, error(1, FILE_COLUMN, MESSAGE_FILE_UTF8));
        } catch (IOException exception) {
            throw new BusinessException(500, "job import file unavailable");
        }
    }

    private void validateFileBoundary(String fileName, MultipartFile file) {
        if (file == null) {
            throw validationFailure(fileName, 0, error(1, FILE_COLUMN, MESSAGE_FILE_REQUIRED));
        }
        if (fileName.isBlank()) {
            throw validationFailure(fileName, 0, error(1, FILE_COLUMN, MESSAGE_FILE_NAME));
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw validationFailure(fileName, 0, error(1, FILE_COLUMN, MESSAGE_FILE_TYPE));
        }
        if (file.isEmpty()) {
            throw validationFailure(fileName, 0, error(1, FILE_COLUMN, MESSAGE_FILE_EMPTY));
        }
    }

    private Reader utf8Reader(MultipartFile file) throws IOException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        return new BufferedReader(new InputStreamReader(file.getInputStream(), decoder));
    }

    private List<String> normalizeHeaderNames(List<String> rawHeaders) {
        return rawHeaders.stream()
                .map(this::normalizeHeaderName)
                .toList();
    }

    private String normalizeHeaderName(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim();
    }

    private void validateHeaders(String fileName, List<String> headerNames) {
        Map<String, Long> counts = headerNames.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<AdminJobImportValidationError> errors = new ArrayList<>();

        for (String header : REQUIRED_HEADERS) {
            long count = counts.getOrDefault(header, 0L);
            if (count == 0L) {
                errors.add(error(1, header, MESSAGE_MISSING_HEADER));
            } else if (count > 1L) {
                errors.add(error(1, header, MESSAGE_DUPLICATE_HEADER));
            }
        }

        for (String header : OPTIONAL_HEADERS) {
            if (counts.getOrDefault(header, 0L) > 1L) {
                errors.add(error(1, header, MESSAGE_DUPLICATE_HEADER));
            }
        }

        for (String header : counts.keySet()) {
            if (!header.isBlank() && !APPROVED_HEADERS.contains(header)) {
                errors.add(error(1, header, MESSAGE_UNSUPPORTED_HEADER));
            }
        }

        if (!errors.isEmpty()) {
            throw validationFailure(fileName, 0, errors);
        }
    }

    private void validateRowCount(String fileName, int rowCount) {
        if (rowCount > MAX_DATA_ROWS) {
            throw validationFailure(fileName, rowCount, error(1, FILE_COLUMN, MESSAGE_ROW_LIMIT));
        }
    }

    private JobImportRow toRow(int rowNumber, CSVRecord record, Set<String> availableHeaders) {
        return new JobImportRow(
                rowNumber,
                cell(record, availableHeaders, "title"),
                cell(record, availableHeaders, "companyName"),
                cell(record, availableHeaders, "city"),
                cell(record, availableHeaders, "jobType"),
                cell(record, availableHeaders, "educationRequirement"),
                cell(record, availableHeaders, "sourcePlatform"),
                cell(record, availableHeaders, "sourceUrl"),
                cell(record, availableHeaders, "summary"),
                cell(record, availableHeaders, "content"),
                cell(record, availableHeaders, "deadlineAt"));
    }

    private String cell(CSVRecord record, Set<String> availableHeaders, String header) {
        if (!availableHeaders.contains(header)) {
            return null;
        }
        try {
            return record.get(header);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private JobImportValidationException validationFailure(
            String fileName,
            int totalRows,
            AdminJobImportValidationError error) {
        return validationFailure(fileName, totalRows, List.of(error));
    }

    private JobImportValidationException validationFailure(
            String fileName,
            int totalRows,
            List<AdminJobImportValidationError> errors) {
        return new JobImportValidationException(new AdminJobImportValidationResponse(
                fileName,
                totalRows,
                0,
                List.copyOf(errors)));
    }

    private AdminJobImportValidationError error(int rowNumber, String column, String message) {
        return new AdminJobImportValidationError(rowNumber, column, message);
    }

    private String displayFileName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            return "";
        }
        return file.getOriginalFilename().trim();
    }
}
