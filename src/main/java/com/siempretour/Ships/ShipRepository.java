package com.siempretour.Ships;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipRepository extends JpaRepository<Ship, Long> {
    Optional<Ship> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Ship> findAllByOrderByCompanyAscNameAsc();
}
