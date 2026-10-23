package pe.edu.cibertec.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.assignment.entity.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    boolean existsByAssetIdAndStatus(String assetId, String status);
}
