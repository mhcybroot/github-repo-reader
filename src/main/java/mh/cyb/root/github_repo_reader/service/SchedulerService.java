package mh.cyb.root.github_repo_reader.service;

import mh.cyb.root.github_repo_reader.model.Contributor;
import mh.cyb.root.github_repo_reader.repository.ContributorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final GitService gitService;
    private final ContentParserService contentParserService;
    private final ContributorRepository contributorRepository;
    private final GithubApiClient githubApiClient;

    public SchedulerService(GitService gitService, ContentParserService contentParserService,
            ContributorRepository contributorRepository, GithubApiClient githubApiClient) {
        this.gitService = gitService;
        this.contentParserService = contentParserService;
        this.contributorRepository = contributorRepository;
        this.githubApiClient = githubApiClient;
    }

   
    @Scheduled(fixedRateString = "${github.scheduler.rate}", initialDelay = 5000)
    public void scheduleTask() {
        logger.info("Starting scheduled task to sync github repo...");
        try {
           
            gitService.cloneOrPullRepository();           
            contentParserService.parseAndSaveContent();
            updateContributors();
            logger.info("Scheduled task completed successfully.");
        } catch (Exception e) {
            logger.error("Error during scheduled task", e);
        }
    }

    @Transactional
    public void updateContributors() {
        List<Contributor> contributors = githubApiClient.fetchContributorsWithMetadata();

        if (contributors.isEmpty()) {
            logger.warn("GitHub API returned no contributors. Falling back to GitService parsing...");
            contributors = gitService.getContributors();
        }

        
        if (!contributors.isEmpty()) {
            
            String repoName = contributors.get(0).getRepoName();
            List<Contributor> existing = contributorRepository.findByRepoName(repoName);
            contributorRepository.deleteAll(existing);
        }

        for (Contributor c : contributors) {
            contributorRepository.save(c);
        }
    }
}
