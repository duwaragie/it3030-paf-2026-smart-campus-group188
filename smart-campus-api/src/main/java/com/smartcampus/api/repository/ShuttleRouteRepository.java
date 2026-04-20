package com.smartcampus.api.repository;

import com.smartcampus.api.model.ShuttleRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShuttleRouteRepository extends JpaRepository<ShuttleRoute, Long> {
    List<ShuttleRoute> findByActiveTrue();
}
