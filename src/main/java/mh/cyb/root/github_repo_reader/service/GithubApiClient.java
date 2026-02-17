package mh.cyb.root.github_repo_reader.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mh.cyb.root.github_repo_reader.model.Contributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class GithubApiClient {

    private static final Logger logger = LoggerFactory.getLogger(GithubApiClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${github.repo.url}")
    private String repoUrl;

    public GithubApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public List<Contributor> fetchContributorsWithMetadata() {
        List<Contributor> contributors = new ArrayList<>();
        try {
            String cleanUrl = repoUrl.endsWith(".git") ? repoUrl.substring(0, repoUrl.length() - 4) : repoUrl;
            String[] parts = cleanUrl.split("/");
            if (parts.length < 2) {
                logger.error("Invalid GitHub URL: {}", repoUrl);
                return contributors;
            }
            String repo = parts[parts.length - 1];
            String owner = parts[parts.length - 2];
            String repoName = repo;

            String repoApiUrl = String.format("https://api.github.com/repos/%s/%s", owner, repo);
            String description = null;
            String htmlUrl = cleanUrl;
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(repoApiUrl)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode node = objectMapper.readTree(response.body());
                    if (node.has("description") && !node.get("description").isNull()) {
                        description = node.get("description").asText();
                    }
                    if (node.has("html_url")) {
                        htmlUrl = node.get("html_url").asText();
                    }
                } else {
                    logger.error("Failed to fetch repo details: {}", response.statusCode());
                }
            } catch (Exception e) {
                logger.error("Error fetching repo details", e);
            }

            String contribApiUrl = String.format("https://api.github.com/repos/%s/%s/contributors", owner, repo);
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(contribApiUrl)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    if (root.isArray()) {
                        for (JsonNode node : root) {
                            Contributor c = new Contributor();
                            c.setUserName(node.get("login").asText());
                            c.setContributionCount(node.get("contributions").asInt());
                            c.setProfileUrl(node.get("avatar_url").asText());
                            c.setRepoName(repoName);
                            c.setRepoDescription(description);
                            c.setRepoUrl(htmlUrl);

                            contributors.add(c);
                        }
                    }
                } else {
                    logger.error("Failed to fetch contributors: {}", response.statusCode());
                }
            } catch (Exception e) {
                logger.error("Error fetching contributors", e);
            }

        } catch (Exception e) {
            logger.error("Error in fetchContributorsWithMetadata", e);
        }
        return contributors;
    }
}
