package com.siempretour.Booking;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByTourId(Long tourId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByTourIdAndStatus(Long tourId, BookingStatus status);

    // Pending bookings for admin
    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status);
}