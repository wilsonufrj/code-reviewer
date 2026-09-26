package com.codereviewer.application.web;

import com.codereviewer.application.dto.AuthorInfoResponse;
import com.codereviewer.application.dto.RepositoryInfoResponse;
import com.codereviewer.application.service.RepositoryInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RepositoryInfoControllerTest {

    private static final Instant DATE = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private RepositoryInfoService repositoryInfoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RepositoryInfoController controller = new RepositoryInfoController(repositoryInfoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnRepositoryInformation() throws Exception {
        when(repositoryInfoService.getRepository("owner", "repo")).thenReturn(repositoryResponse());

        mockMvc.perform(get("/api/repository-info/repositories/owner/repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("owner/repo"))
                .andExpect(jsonPath("$.owner.login").value("owner"))
                .andExpect(jsonPath("$.authors[0].login").value("author"));
    }

    @Test
    void shouldReturnAuthorInformationAndRepositories() throws Exception {
        AuthorInfoResponse response = new AuthorInfoResponse(
                2L, "author", "Author Name", "avatar", "profile", "bio", null, null, null,
                1, 10, 3, DATE, DATE,
                List.of(new AuthorInfoResponse.Repository(
                        1L, "repo", "owner/repo", "description", "url", "main", "Java",
                        "public", false, false, 5, 2, 1, DATE, DATE, DATE)));
        when(repositoryInfoService.getAuthor("author")).thenReturn(response);

        mockMvc.perform(get("/api/repository-info/authors/author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("author"))
                .andExpect(jsonPath("$.repositories[0].fullName").value("owner/repo"));
    }

    private RepositoryInfoResponse repositoryResponse() {
        return new RepositoryInfoResponse(
                1L, "repo", "owner/repo", "description", "url", "main", "Java", "public",
                false, false, 5, 2, 1, DATE, DATE, DATE,
                new RepositoryInfoResponse.Owner(1L, "owner", "User", "avatar", "profile"),
                List.of(new RepositoryInfoResponse.Author(
                        2L, "author", "avatar", "profile", 12)));
    }
}
