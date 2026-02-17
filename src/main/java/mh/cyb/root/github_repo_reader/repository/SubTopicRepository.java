package mh.cyb.root.github_repo_reader.repository;

import mh.cyb.root.github_repo_reader.model.SubTopic;
import mh.cyb.root.github_repo_reader.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubTopicRepository extends JpaRepository<SubTopic, Long> {
    List<SubTopic> findByTopic(Topic topic);

    Optional<SubTopic> findByTopicAndName(Topic topic, String name);
}
