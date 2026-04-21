package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.campus.common.JobImportValidationException;
import com.campus.dto.AdminJobImportValidationError;

class JobImportCsvParserTests {

    private final JobImportCsvParser parser = new JobImportCsvParser();

    @Test
    void parsesUtf8CsvWithQuotedCellsAndExpectedRowNumbers() {
        MockMultipartFile file = csvFile("jobs.csv", """
                title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                "Data Intern","Campus, Inc",Hangzhou,internship,bachelor,Official Site,https://jobs.example.com/data-intern,"Build dashboards","quoted, content",2026-06-20 18:00:00
                """);

        assertThat(parser.parse(file))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.rowNumber()).isEqualTo(2);
                    assertThat(row.companyName()).isEqualTo("Campus, Inc");
                    assertThat(row.content()).isEqualTo("quoted, content");
                    assertThat(row.deadlineAt()).isEqualTo("2026-06-20 18:00:00");
                });
    }

    @Test
    void rejectsMissingRequiredHeader() {
        MockMultipartFile file = csvFile("jobs.csv", """
                title,companyName,city,jobType,educationRequirement,sourcePlatform,summary
                Data Intern,Campus Inc,Hangzhou,INTERNSHIP,BACHELOR,Official Site,Summary
                """);

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                    assertThat(exception.response().totalRows()).isEqualTo(0);
                    assertThat(exception.response().errors())
                            .extracting(AdminJobImportValidationError::column)
                            .contains("sourceUrl");
                });
    }

    @Test
    void rejectsFilesAboveRowLimit() {
        MockMultipartFile file = csvFile("jobs.csv", buildCsvWithRows(201));

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                    assertThat(exception.response().totalRows()).isEqualTo(201);
                    assertThat(exception.response().errors())
                            .extracting(AdminJobImportValidationError::message)
                            .containsExactly("job import row limit exceeded");
                });
    }

    @Test
    void rejectsMalformedUtf8Payload() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                new byte[] { (byte) 0xC3, (byte) 0x28 });

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                    assertThat(exception.response().errors())
                            .extracting(AdminJobImportValidationError::message)
                            .containsExactly("csv file must be utf-8 encoded");
                });
    }

    private MockMultipartFile csvFile(String fileName, String body) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/csv",
                body.getBytes(StandardCharsets.UTF_8));
    }

    private String buildCsvWithRows(int rowCount) {
        String header = "title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt";
        String rows = IntStream.range(0, rowCount)
                .mapToObj(index -> String.format(
                        "Title %1$d,Company %1$d,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/import/%1$d,Summary %1$d,Content %1$d,2026-06-20 18:00:00",
                        index + 1))
                .collect(Collectors.joining(System.lineSeparator()));
        return header + System.lineSeparator() + rows + System.lineSeparator();
    }
}
