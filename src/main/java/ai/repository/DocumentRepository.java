package ai.repository;

import ai.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByIdAndUserId(Long id, Long userId);

    List<DocumentEntity> findAllByUserId(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}