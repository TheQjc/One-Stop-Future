package com.campus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CommunityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void guestCanReadCommunityListAndFilterByTag() throws Exception {
        mockMvc.perform(get("/api/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.posts[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data.posts[0].experience.enabled").value(true))
                .andExpect(jsonPath("$.data.posts[0].experience.targetLabel").value("雅思7.5分冲刺"));

        mockMvc.perform(get("/api/community/posts").param("tag", "EXAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.selectedTag").value("EXAM"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.posts[0].tag").value("EXAM"))
                .andExpect(jsonPath("$.data.posts[0].experience.enabled").value(false))
                .andExpect(jsonPath("$.data.posts[0].experience.targetLabel").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void guestCanReadCommunityDetail() throws Exception {
        mockMvc.perform(get("/api/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("实习求职时间线记录"))
                .andExpect(jsonPath("$.data.comments").isArray())
                .andExpect(jsonPath("$.data.comments[0].content").value("欢迎在此分享你的面试准备里程碑。"))
                .andExpect(jsonPath("$.data.comments[0].replies[0].content")
                        .value("我每周进行一次模拟面试，效果非常显著。"))
                .andExpect(jsonPath("$.data.comments[0].replies[0].replyToUserId").value(2))
                .andExpect(jsonPath("$.data.comments[0].replies[0].replyToUserNickname").value("普通用户"));
    }

    @Test
    void legacyNonExperiencePostsStillReturnDisabledExperienceBlock() throws Exception {
        mockMvc.perform(get("/api/community/posts/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.experience.enabled").value(false))
                .andExpect(jsonPath("$.data.experience.targetLabel").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.experience.actionSummary").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void guestCanReadCommunityHotBoardWithWeekDefault() throws Exception {
        mockMvc.perform(get("/api/community/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].hotLabel").value("本周热议"))
                .andExpect(jsonPath("$.data.items[1].id").value(2));
    }

    @Test
    void dayWeekAndAllBoardsUseRollingPublishWindows() throws Exception {
        jdbcTemplate.update("UPDATE t_community_post SET created_at = TIMESTAMPADD(DAY, -9, CURRENT_TIMESTAMP) WHERE id = 1");
        jdbcTemplate.update("UPDATE t_community_post SET created_at = TIMESTAMPADD(HOUR, -6, CURRENT_TIMESTAMP) WHERE id = 2");
        jdbcTemplate.update("UPDATE t_community_post SET created_at = TIMESTAMPADD(HOUR, -20, CURRENT_TIMESTAMP) WHERE id = 3");

        mockMvc.perform(get("/api/community/hot").param("period", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.period").value("DAY"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(2))
                .andExpect(jsonPath("$.data.items[1].id").value(3));

        mockMvc.perform(get("/api/community/hot").param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/community/hot").param("period", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(2))
                .andExpect(jsonPath("$.data.items[1].id").value(1));
    }

    @Test
    void hotBoardSortsByHeatThenRecencyAndRejectsInvalidParams() throws Exception {
        jdbcTemplate.update("UPDATE t_community_post SET like_count = 3, favorite_count = 0, created_at = TIMESTAMPADD(HOUR, -5, CURRENT_TIMESTAMP) WHERE id = 1");
        jdbcTemplate.update("UPDATE t_community_post SET like_count = 3, favorite_count = 0, created_at = TIMESTAMPADD(HOUR, -1, CURRENT_TIMESTAMP) WHERE id = 2");

        mockMvc.perform(get("/api/community/hot").param("period", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].id").value(2))
                .andExpect(jsonPath("$.data.items[1].id").value(1));

        mockMvc.perform(get("/api/community/hot").param("period", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无效的社区热门时间范围"));

        mockMvc.perform(get("/api/community/hot").param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无效的社区热门数量限制"));
    }

    @Test
    void guestCannotCreatePost() throws Exception {
        mockMvc.perform(post("/api/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tag":"CAREER","title":"Offer review","content":"Body"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanCreatePostAndReadMine() throws Exception {
        mockMvc.perform(post("/api/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tag":"CHAT","title":"Campus notes","content":"Plain text content for the forum."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tag").value("CHAT"))
                .andExpect(jsonPath("$.data.title").value("Campus notes"))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        mockMvc.perform(get("/api/community/posts/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanCreateExperiencePostWithStructuredFields() throws Exception {
        mockMvc.perform(post("/api/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tag": "CAREER",
                                  "title": "Campus recruiting recap",
                                  "content": "Focus on project proof before pushing volume.",
                                  "experiencePost": true,
                                  "experienceTargetLabel": "  Backend internship sprint  ",
                                  "experienceOutcomeLabel": "Received 2 interview invitations",
                                  "experienceTimelineSummary": "Week 1 resume refresh, week 2 projects, week 3 applications",
                                  "experienceActionSummary": "Refine one showcase project, then batch tailored applications."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tag").value("CAREER"))
                .andExpect(jsonPath("$.data.experience.enabled").value(true))
                .andExpect(jsonPath("$.data.experience.targetLabel").value("Backend internship sprint"))
                .andExpect(jsonPath("$.data.experience.outcomeLabel").value("Received 2 interview invitations"))
                .andExpect(jsonPath("$.data.experience.timelineSummary").value("Week 1 resume refresh, week 2 projects, week 3 applications"))
                .andExpect(jsonPath("$.data.experience.actionSummary").value("Refine one showcase project, then batch tailored applications."));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_experience_post FROM t_community_post WHERE id = 4", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT experience_target_label FROM t_community_post WHERE id = 4", String.class))
                .isEqualTo("Backend internship sprint");

        mockMvc.perform(get("/api/community/posts/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.experience.enabled").value(true))
                .andExpect(jsonPath("$.data.experience.targetLabel").value("Backend internship sprint"));

        mockMvc.perform(get("/api/community/posts/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.posts[0].title").value("Campus recruiting recap"))
                .andExpect(jsonPath("$.data.posts[0].experience.enabled").value(true));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanCommentLikeAndFavoriteWithIdempotentInteractions() throws Exception {
        mockMvc.perform(post("/api/community/posts/2/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Useful planning summary."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.commentCount").value(1))
                .andExpect(jsonPath("$.data.comments[0].content").value("Useful planning summary."));

        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM t_notification
                        WHERE user_id = 3 AND type = 'COMMUNITY_COMMENT_RECEIVED'
                          AND source_type = 'COMMUNITY_POST' AND source_id = 2
                        """,
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT content FROM t_notification
                        WHERE user_id = 3 AND type = 'COMMUNITY_COMMENT_RECEIVED'
                          AND source_type = 'COMMUNITY_POST' AND source_id = 2
                        """,
                String.class)).contains("NormalUser").contains("Exam planning checklist");

        mockMvc.perform(post("/api/community/posts/2/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(true));

        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM t_notification
                        WHERE user_id = 3 AND type = 'COMMUNITY_POST_LIKED'
                          AND source_type = 'COMMUNITY_POST' AND source_id = 2
                        """,
                Integer.class)).isEqualTo(1);

        mockMvc.perform(post("/api/community/posts/2/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(true));

        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM t_notification
                        WHERE user_id = 3 AND type = 'COMMUNITY_POST_LIKED'
                          AND source_type = 'COMMUNITY_POST' AND source_id = 2
                        """,
                Integer.class)).isEqualTo(1);

        mockMvc.perform(post("/api/community/posts/2/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favoriteCount").value(1))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true));

        mockMvc.perform(delete("/api/community/posts/2/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favoriteCount").value(0))
                .andExpect(jsonPath("$.data.favoritedByMe").value(false));

        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT like_count FROM t_community_post WHERE id = 2", Integer.class);
        Integer commentCount = jdbcTemplate.queryForObject(
                "SELECT comment_count FROM t_community_post WHERE id = 2", Integer.class);

        assertThat(likeCount).isEqualTo(1);
        assertThat(commentCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void selfLikeDoesNotCreateNotification() throws Exception {
        mockMvc.perform(post("/api/community/posts/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedByMe").value(true));

        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM t_notification
                        WHERE user_id = 2 AND type = 'COMMUNITY_POST_LIKED'
                          AND source_type = 'COMMUNITY_POST' AND source_id = 1
                        """,
                Integer.class)).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "3", roles = "USER")
    void authenticatedUserCanReplyToTopLevelCommentAndTriggerNotification() throws Exception {
        mockMvc.perform(post("/api/community/comments/1/replies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Try batching your prep notes by interview stage."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.comments[0].replies[1].content")
                        .value("Try batching your prep notes by interview stage."))
                .andExpect(jsonPath("$.data.comments[0].replies[1].replyToUserNickname").value("普通用户"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE user_id = 2 AND type = 'COMMUNITY_REPLY_RECEIVED'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT content FROM t_notification WHERE user_id = 2 AND type = 'COMMUNITY_REPLY_RECEIVED'",
                String.class)).contains("认证用户").contains("实习求职时间线记录");
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void selfReplyDoesNotCreateNotification() throws Exception {
        mockMvc.perform(post("/api/community/comments/1/replies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Adding my own follow-up note."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.comments[0].replies[1].content").value("Adding my own follow-up note."));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE user_id = 2 AND type = 'COMMUNITY_REPLY_RECEIVED'",
                Integer.class)).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "3", roles = "USER")
    void replyingToAReplyIsRejected() throws Exception {
        mockMvc.perform(post("/api/community/comments/2/replies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Third-level threads are out of scope."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无法在回复下直接回复"));
    }

    @Test
    void hiddenPostsAreNotVisibleToPublicReaders() throws Exception {
        jdbcTemplate.update("UPDATE t_community_post SET status = 'HIDDEN' WHERE id = 1");

        mockMvc.perform(get("/api/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }
}
