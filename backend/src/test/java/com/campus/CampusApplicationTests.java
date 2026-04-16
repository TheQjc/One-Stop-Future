package com.campus;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Locale;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void phaseABaseCommunityJobAndResourceTablesExist() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(tableExists(metaData, "t_user")).isTrue();
            assertThat(tableExists(metaData, "t_verification_code")).isTrue();
            assertThat(tableExists(metaData, "t_verification_application")).isTrue();
            assertThat(tableExists(metaData, "t_notification")).isTrue();
            assertThat(tableExists(metaData, "t_community_post")).isTrue();
            assertThat(tableExists(metaData, "t_community_comment")).isTrue();
            assertThat(tableExists(metaData, "t_community_post_like")).isTrue();
            assertThat(tableExists(metaData, "t_user_favorite")).isTrue();
            assertThat(tableExists(metaData, "t_job_posting")).isTrue();
            assertThat(tableExists(metaData, "t_resource_item")).isTrue();
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws Exception {
        try (ResultSet lower = metaData.getTables(null, null, tableName, null)) {
            if (lower.next()) {
                return true;
            }
        }
        try (ResultSet upper = metaData.getTables(null, null, tableName.toUpperCase(Locale.ROOT), null)) {
            return upper.next();
        }
    }
}
