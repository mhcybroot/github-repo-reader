package mh.cyb.root.github_repo_reader.controller;

import mh.cyb.root.github_repo_reader.dto.ApiResponse;
import mh.cyb.root.github_repo_reader.service.GithubReaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final GithubReaderService githubReaderService;

    public ApiController(GithubReaderService githubReaderService) {
        this.githubReaderService = githubReaderService;
    }

    @GetMapping("/read_contributions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> readContributions() {
        try {
            List<Map<String, Object>> contributions = githubReaderService.getContributions();
            return ResponseEntity.ok(ApiResponse.success(contributions, "Read successful"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to read contributions: " + e.getMessage()));
        }
    }

    @GetMapping("/read_indices")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> readIndices() {
        try {
            List<Map<String, Object>> indices = githubReaderService.getIndices();
            return ResponseEntity.ok(ApiResponse.success(indices, "Read successful"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to read indices: " + e.getMessage()));
        }
    }

    @GetMapping("/read_blog")
    public ResponseEntity<ApiResponse<Map<String, String>>> readBlog(
            @RequestParam("topic_name") String topicName,
            @RequestParam("sub_topic_name") String subTopicName) {
        try {
            Map<String, String> content = githubReaderService.getBlogContent(topicName, subTopicName);
            return ResponseEntity.ok(ApiResponse.success(content, "Read successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to read blog: " + e.getMessage()));
        }
    }
}
