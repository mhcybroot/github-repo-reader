package mh.cyb.root.github_repo_reader.service;

import mh.cyb.root.github_repo_reader.model.Contributor;
import mh.cyb.root.github_repo_reader.model.SubTopic;
import mh.cyb.root.github_repo_reader.model.Topic;
import mh.cyb.root.github_repo_reader.repository.ContributorRepository;
import mh.cyb.root.github_repo_reader.repository.SubTopicRepository;
import mh.cyb.root.github_repo_reader.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GithubReaderService {

    private final TopicRepository topicRepository;
    private final SubTopicRepository subTopicRepository;
    private final ContributorRepository contributorRepository;

    public GithubReaderService(TopicRepository topicRepository, SubTopicRepository subTopicRepository,
            ContributorRepository contributorRepository) {
        this.topicRepository = topicRepository;
        this.subTopicRepository = subTopicRepository;
        this.contributorRepository = contributorRepository;
    }

    public List<Map<String, Object>> getIndices() {
        List<Topic> topics = topicRepository.findAll();
       
        return topics.stream().map(topic -> {
            Map<String, Object> topicMap = new HashMap<>();
            topicMap.put("topic_name", topic.getName());

            List<SubTopic> subTopics = subTopicRepository.findByTopic(topic);
            topicMap.put("no_of_sub_topics", subTopics.size());

            List<Map<String, String>> subTopicList = subTopics.stream().map(st -> {
                Map<String, String> stMap = new HashMap<>();
                stMap.put("sub_topic_name", st.getName());
                return stMap;
            }).collect(Collectors.toList());

            topicMap.put("subTopicList", subTopicList);
            return topicMap;
        }).collect(Collectors.toList());
    }

    public Map<String, String> getBlogContent(String topicName, String subTopicName) {
        Topic topic = topicRepository.findByName(topicName)
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicName));

        SubTopic subTopic = subTopicRepository.findByTopicAndName(topic, subTopicName)
                .or(() -> subTopicRepository.findByTopicAndName(topic, topicName)) 
                                                                                   
                .orElseThrow(() -> new RuntimeException("SubTopic not found: " + subTopicName));

        Map<String, String> response = new HashMap<>();
        response.put("topic_name", topic.getName());
        response.put("sub_topic_name", subTopic.getName());
        response.put("content", subTopic.getContent());
        return response;
    }

    public List<Map<String, Object>> getContributions() {
        List<Contributor> contributors = contributorRepository.findAll();

      
        Map<String, List<Contributor>> grouped = contributors.stream()
                .collect(Collectors.groupingBy(Contributor::getRepoName));

        List<Map<String, Object>> result = new ArrayList<>();

        grouped.forEach((repoName, list) -> {
            Map<String, Object> repoMap = new HashMap<>();
            repoMap.put("name", repoName);
           
            Contributor first = list.get(0);
            repoMap.put("description", first.getRepoDescription());
            repoMap.put("url", first.getRepoUrl());

            List<Map<String, Object>> contribList = list.stream().map(c -> {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("user_name", c.getUserName());
                cMap.put("contribution_count", c.getContributionCount());
                cMap.put("profile_url", c.getProfileUrl());
                return cMap;
            }).collect(Collectors.toList());

            repoMap.put("contribution", contribList);
            result.add(repoMap);
        });

        return result;
    }
}
