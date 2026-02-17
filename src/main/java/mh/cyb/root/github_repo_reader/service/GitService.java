package mh.cyb.root.github_repo_reader.service;

import mh.cyb.root.github_repo_reader.model.Contributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitService {

    private static final Logger logger = LoggerFactory.getLogger(GitService.class);

    @Value("${github.repo.url}")
    private String repoUrl;

    @Value("${github.local.path}")
    private String localPath;

    public void cloneOrPullRepository() throws IOException, InterruptedException {
        File repoDir = new File(localPath);
        if (repoDir.exists() && new File(repoDir, ".git").exists()) {
            logger.info("Repository exists at {}. Pulling latest changes...", localPath);
            executeGitCommand(repoDir, "pull");
        } else {
            logger.info("Cloning repository from {} to {}...", repoUrl, localPath);
            File parentDir = repoDir.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            executeGitCommand(parentDir != null ? parentDir : new File("."), "clone", repoUrl, repoDir.getName());
        }
    }

    public List<Contributor> getContributors() {
        List<Contributor> contributors = new ArrayList<>();
        File repoDir = new File(localPath);
        if (!repoDir.exists()) {
            logger.error("Repository not found at {}", localPath);
            return contributors;
        }

        try {
            
            List<String> output = executeGitCommand(repoDir, "shortlog", "-sne", "--all");

            Pattern pattern = Pattern.compile("\\s*(\\d+)\\s+(.+)\\s+<(.+)>");

            for (String line : output) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int count = Integer.parseInt(matcher.group(1));
                    String name = matcher.group(2).trim();
                    String email = matcher.group(3).trim();

                  
                    String profileUrl = "https://github.com/" + name.replace(" ", ""); 
                   
                    Contributor contributor = new Contributor();
                    contributor.setUserName(name);
                    contributor.setContributionCount(count);
                    contributor.setProfileUrl(profileUrl);
                    contributor.setRepoName(repoDir.getName());
                    contributors.add(contributor);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting contributors", e);
        }
        return contributors;
    }

    private List<String> executeGitCommand(File workingDir, String... command)
            throws IOException, InterruptedException {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add("git");
     
        for (String arg : command) {
            fullCommand.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        List<String> output = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
                logger.debug("Git output: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(
                    "Git command failed with exit code " + exitCode + ": " + String.join(" ", command));
        }
        return output;
    }
}
