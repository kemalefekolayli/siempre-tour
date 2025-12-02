package Booking;

import Tours.Models.Tour;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false, length = 100)
    private String userId; // Supabase user ID

    @Column(nullable = false, length = 100)
    private String userEmail;

    @Column(length = 100)
    private String userName;

    @Column(length = 20)
    private String userPhone;

    @NotNull
    @Min(value = 1)
    @Column(nullable = false)
    private Integer numberOfPeople; // Kaç kişilik rezervasyon

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String userMessage; // Kullanıcının notu/mesajı

    @Column(columnDefinition = "TEXT")
    private String adminNote; // Admin'in notu (red nedeni vs)

    // Approve/Reject bilgileri
    @Column(length = 100)
    private String approvedBy; // Admin user ID

    private LocalDateTime approvedAt;

    @Column(length = 100)
    private String rejectedBy; // Admin user ID

    private LocalDateTime rejectedAt;

    // Audit
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}