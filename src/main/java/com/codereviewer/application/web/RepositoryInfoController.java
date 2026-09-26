package com.codereviewer.application.web;

import com.codereviewer.application.dto.AuthorInfoResponse;
import com.codereviewer.application.dto.RepositoryInfoResponse;
import com.codereviewer.application.service.RepositoryInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for public GitHub repository and author information. */
@RestController
@RequestMapping("/api/repository-info")
@RequiredArgsConstructor
@Tag(name = "Repository information", description = "Query public GitHub repository and author metadata")
public class RepositoryInfoController {

    private final RepositoryInfoService repositoryInfoService;

    @GetMapping("/repositories/{owner}/{repo}")
    @Operation(summary = "Get repository information and contributors")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repository information returned"),
            @ApiResponse(responseCode = "404", description = "GitHub repository not found"),
            @ApiResponse(responseCode = "502", description = "GitHub API request failed")
    })
    public RepositoryInfoResponse getRepository(@PathVariable String owner,
                                                @PathVariable String repo) {
        return repositoryInfoService.getRepository(owner, repo);
    }

    @GetMapping("/authors/{username}")
    @Operation(summary = "Get an author profile and owned repositories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author information returned"),
            @ApiResponse(responseCode = "404", description = "GitHub author not found"),
            @ApiResponse(responseCode = "502", description = "GitHub API request failed")
    })
    public AuthorInfoResponse getAuthor(@PathVariable String username) {
        return repositoryInfoService.getAuthor(username);
    }
}
