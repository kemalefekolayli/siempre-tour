package Booking;

public enum BookingStatus {
    PENDING,      // Beklemede - Admin onayı bekleniyor
    APPROVED,     // Onaylandı
    REJECTED,     // Reddedildi
    CANCELLED     // Kullanıcı iptal etti
}
