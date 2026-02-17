package mh.cyb.root.github_repo_reader.repository;

import mh.cyb.root.github_repo_reader.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    Optional<Topic> findByName(String name);

    Optional<Topic> findByDirectoryName(String directoryName);
}
