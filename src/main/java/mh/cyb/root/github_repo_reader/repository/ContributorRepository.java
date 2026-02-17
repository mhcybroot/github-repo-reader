package mh.cyb.root.github_repo_reader.repository;

import mh.cyb.root.github_repo_reader.model.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, Long> {
    List<Contributor> findByRepoName(String repoName);

    Optional<Contributor> findByRepoNameAndUserName(String repoName, String userName);

    void deleteByRepoName(String repoName);
}
