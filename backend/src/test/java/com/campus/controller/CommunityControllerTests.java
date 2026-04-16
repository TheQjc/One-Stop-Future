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
                .andExpect(jsonPath("$.data.posts[0].title").isNotEmpty());

        mockMvc.perform(get("/api/community/posts").param("tag", "EXAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.selectedTag").value("EXAM"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.posts[0].tag").value("EXAM"));
    }

    @Test
    void guestCanReadCommunityDetail() throws Exception {
        mockMvc.perform(get("/api/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Offer timeline notes"))
                .andExpect(jsonPath("$.data.comments").isArray());
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

        mockMvc.perform(post("/api/community/posts/2/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(true));

        mockMvc.perform(post("/api/community/posts/2/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(true));

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
    void hiddenPostsAreNotVisibleToPublicReaders() throws Exception {
        jdbcTemplate.update("UPDATE t_community_post SET status = 'HIDDEN' WHERE id = 1");

        mockMvc.perform(get("/api/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("community post not found"));
    }
}
