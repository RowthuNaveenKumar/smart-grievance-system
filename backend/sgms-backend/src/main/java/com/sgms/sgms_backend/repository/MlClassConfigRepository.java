package com.sgms.sgms_backend.repository;

import com.sgms.sgms_backend.model.MlClassConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ML class resolution configuration.
 * The ml_class_config table drives category routing in CategoryResolutionService.
 */
@Repository
public interface MlClassConfigRepository extends JpaRepository<MlClassConfig, String> {

    /**
     * Find the active resolution config for a given ML class label.
     * Returns empty if the ML class is unknown or has been deactivated.
     */
    Optional<MlClassConfig> findByMlClassAndActiveTrue(String mlClass);

    /**
     * Returns all currently active ML class configurations.
     * Used for admin validation and diagnostics.
     */
    List<MlClassConfig> findByActiveTrue();
}
