package mh.cyb.root.github_repo_reader.service;

import mh.cyb.root.github_repo_reader.model.SubTopic;
import mh.cyb.root.github_repo_reader.model.Topic;
import mh.cyb.root.github_repo_reader.repository.SubTopicRepository;
import mh.cyb.root.github_repo_reader.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContentParserService {

    private static final Logger logger = LoggerFactory.getLogger(ContentParserService.class);

    private final TopicRepository topicRepository;
    private final SubTopicRepository subTopicRepository;

    @Value("${github.local.path}")
    private String localPath;

    public ContentParserService(TopicRepository topicRepository, SubTopicRepository subTopicRepository) {
        this.topicRepository = topicRepository;
        this.subTopicRepository = subTopicRepository;
    }

    @Transactional
    public void parseAndSaveContent() {
        File repoDir = new File(localPath);
        if (!repoDir.exists()) {
            logger.error("Repository directory not found at {}", localPath);
            return;
        }

        File rootReadme = new File(repoDir, "README.md");
        if (!rootReadme.exists()) {
            logger.error("Root README.md not found");
            return;
        }

        List<String> topicOrder = parseTopicOrder(rootReadme);

        for (String topicDirName : topicOrder) {
            File topicDir = new File(repoDir, topicDirName);
            if (topicDir.exists() && topicDir.isDirectory()) {
                String topicDisplayName = getTopicDisplayName(rootReadme, topicDirName);

                Topic topic = topicRepository.findByDirectoryName(topicDirName)
                        .orElse(new Topic(topicDisplayName, topicDirName));

                if (topic.getId() == null) {
                    topic.setName(topicDisplayName);
                }
                topic = topicRepository.save(topic);

                parseSubTopics(topic, topicDir);
            }
        }
    }

    private List<String> parseTopicOrder(File readme) {
        List<String> topics = new ArrayList<>();
        try {
            String content = Files.readString(readme.toPath());

            Pattern pattern = Pattern.compile("### Topic Orders\\s*```\\s*(.*?)\\s*```", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String listStr = matcher.group(1);
                String[] parts = listStr.split(",");
                for (String part : parts) {
                    topics.add(part.trim());
                }
            } else {
               
                logger.warn("Could not find 'Topic Orders' block, trying 'Topic list'...");
            }

           
        } catch (IOException e) {
            logger.error("Error reading README.md", e);
        }
        return topics;
    }

    private String getTopicDisplayName(File readme, String dirName) {
        try {
            String content = Files.readString(readme.toPath());
             Pattern pattern = Pattern.compile("-\\s*\\[(.*?)\\]\\(\\./" + Pattern.quote(dirName) + "/.*?\\)");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }

            Pattern pattern2 = Pattern.compile("-\\s*\\[(.*?)\\]\\(\\./" + Pattern.quote(dirName) + "/?\\)");
            Matcher matcher2 = pattern2.matcher(content);
            if (matcher2.find()) {
                return matcher2.group(1);
            }

        } catch (IOException e) {
            logger.error("Error reading README for display name", e);
        }
        return dirName; 
    }

    private void parseSubTopics(Topic topic, File topicDir) {
        File[] files = topicDir.listFiles(File::isDirectory);

        if (files != null && files.length > 0) {
            Arrays.sort(files);

            boolean foundSubDirs = false;
            for (File subDir : files) {
                File subReadme = new File(subDir, "README.md");
                if (subReadme.exists()) {
                    foundSubDirs = true;
                    saveSubTopic(topic, subDir.getName(), subReadme);
                }
            }

           
            if (!foundSubDirs) {
                File selfReadme = new File(topicDir, "README.md");
                if (selfReadme.exists()) {
                       saveSubTopic(topic, topic.getName(), selfReadme);
                }
            }
        } else {
            File selfReadme = new File(topicDir, "README.md");
            if (selfReadme.exists()) {
                saveSubTopic(topic, topic.getName(), selfReadme);
            }
        }
    }

    private void saveSubTopic(Topic topic, String subTopicName, File readmeFile) {
        try {
            String content = Files.readString(readmeFile.toPath());

            SubTopic subTopic = subTopicRepository.findByTopicAndName(topic, subTopicName)
                    .orElse(new SubTopic(subTopicName, content, topic));

            subTopic.setContent(content);
            subTopicRepository.save(subTopic);

        } catch (IOException e) {
            logger.error("Error reading subtopic readme", e);
        }
    }
}
