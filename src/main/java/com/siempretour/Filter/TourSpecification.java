package com.siempretour.Filter;


import com.siempretour.Tours.Dto.TourFilterDto;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TourSpecification {

    public static Specification<Tour> withFilters(TourFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Text search - name (case-insensitive partial match)
            if (filter.getName() != null && !filter.getName().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + filter.getName().toLowerCase() + "%"
                ));
            }

            // Text search - departure city (case-insensitive partial match)
            if (filter.getDepartureCity() != null && !filter.getDepartureCity().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("departureCity")),
                        "%" + filter.getDepartureCity().toLowerCase() + "%"
                ));
            }

            // Search within destinations list
            if (filter.getDestination() != null && !filter.getDestination().isBlank()) {
                // Join with destinations collection
                Join<Tour, String> destinationsJoin = root.join("destinations", JoinType.LEFT);
                predicates.add(cb.like(
                        cb.lower(destinationsJoin),
                        "%" + filter.getDestination().toLowerCase() + "%"
                ));
                // Make query distinct to avoid duplicates from join
                query.distinct(true);
            }

            // Single category filter
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }

            // Multiple categories filter
            if (filter.getCategories() != null && !filter.getCategories().isEmpty()) {
                predicates.add(root.get("category").in(filter.getCategories()));
            }

            // Single status filter
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // Multiple statuses filter
            if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.getStatuses()));
            }

            // Price range
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            // Has discount filter
            if (filter.getHasDiscount() != null && filter.getHasDiscount()) {
                predicates.add(cb.isNotNull(root.get("discountedPrice")));
                predicates.add(cb.lessThan(root.get("discountedPrice"), root.get("price")));
            }

            // Duration range
            if (filter.getMinDuration() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("duration"), filter.getMinDuration()));
            }
            if (filter.getMaxDuration() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("duration"), filter.getMaxDuration()));
            }

            // Start date range
            if (filter.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), filter.getStartDateFrom()));
            }
            if (filter.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), filter.getStartDateTo()));
            }

            // End date range
            if (filter.getEndDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), filter.getEndDateFrom()));
            }
            if (filter.getEndDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), filter.getEndDateTo()));
            }

            // Available seats range
            if (filter.getMinAvailableSeats() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("availableSeats"), filter.getMinAvailableSeats()));
            }
            if (filter.getMaxAvailableSeats() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("availableSeats"), filter.getMaxAvailableSeats()));
            }

            // Has availability filter
            if (filter.getHasAvailability() != null && filter.getHasAvailability()) {
                predicates.add(cb.greaterThan(root.get("availableSeats"), 0));
            }

            // Participants range (for tours fitting a group size)
            if (filter.getMinParticipants() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxParticipants"), filter.getMinParticipants()));
            }
            if (filter.getMaxParticipants() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minParticipants"), filter.getMaxParticipants()));
            }

            // Ship name (cruise tours)
            if (filter.getShipName() != null && !filter.getShipName().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("shipName")),
                        "%" + filter.getShipName().toLowerCase() + "%"
                ));
            }

            // Ship company (cruise tours)
            if (filter.getShipCompany() != null && !filter.getShipCompany().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("shipCompany")),
                        "%" + filter.getShipCompany().toLowerCase() + "%"
                ));
            }

            // Is active filter
            if (filter.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
            }

            // Is bookable filter (composite condition)
            if (filter.getIsBookable() != null && filter.getIsBookable()) {
                predicates.add(cb.equal(root.get("isActive"), true));
                predicates.add(cb.equal(root.get("status"), TourStatus.PUBLISHED));
                predicates.add(cb.greaterThan(root.get("availableSeats"), 0));

                // Check booking deadline or start date
                LocalDateTime now = LocalDateTime.now();
                predicates.add(cb.or(
                        cb.and(
                                cb.isNotNull(root.get("bookingDeadline")),
                                cb.greaterThan(root.get("bookingDeadline"), now)
                        ),
                        cb.and(
                                cb.isNull(root.get("bookingDeadline")),
                                cb.greaterThan(root.get("startDate"), now)
                        )
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}