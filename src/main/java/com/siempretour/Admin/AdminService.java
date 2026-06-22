package com.siempretour.Admin;

import com.siempretour.Admin.Dto.*;
import com.siempretour.Booking.Booking;
import com.siempretour.Booking.BookingRepository;
import com.siempretour.Contact.ContactMessage;
import com.siempretour.Contact.ContactMessageRepository;
import com.siempretour.Exceptions.ErrorCodes;
import com.siempretour.Exceptions.GlobalException;
import com.siempretour.Filter.PagedResponse;
import com.siempretour.Tours.Dto.TourFilterDto;
import com.siempretour.Tours.Models.Tour;
import com.siempretour.Tours.Models.TourCategory;
import com.siempretour.Tours.Models.TourStatus;
import com.siempretour.Tours.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BookingRepository bookingRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final TourRepository tourRepository;

    public AdminSummaryDto getSummary(LocalDate startDate, LocalDate endDate, Long tourId, TourCategory category, String requestType) {
        LocalDateTime start = startDateTime(startDate);
        LocalDateTime end = endDateTime(endDate);
        List<Booking> bookings = includeReservations(requestType) ? bookingRepository.findAdminRequests(start, end, tourId, category) : List.of();
        List<ContactMessage> contacts = includeInformation(requestType) && tourId == null && category == null
                ? contactMessageRepository.findAdminRequests(start, end)
                : List.of();

        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        long thisMonthReservations = bookings.stream().filter(b -> !b.getCreatedAt().isBefore(monthStart)).count();
        long thisMonthInformation = contacts.stream().filter(c -> !c.getCreatedAt().isBefore(monthStart)).count();

        return AdminSummaryDto.builder()
                .totalReservationRequests(bookings.size())
                .totalInformationRequests(contacts.size())
                .thisMonthRequests(thisMonthReservations + thisMonthInformation)
                .mostRequestedTour(topTours(bookings, 1).stream().findFirst().orElse(null))
                .mostPopularCategory(topCategories(bookings, 1).stream().findFirst().orElse(null))
                .genderDataAvailable(false)
                .ageDataAvailable(false)
                .build();
    }

    public List<AdminTimeSeriesPointDto> getRequestsOverTime(LocalDate startDate, LocalDate endDate, Long tourId, TourCategory category, String requestType) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(29);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        List<Booking> bookings = includeReservations(requestType) ? bookingRepository.findAdminRequests(startDateTime, endDateTime, tourId, category) : List.of();
        List<ContactMessage> contacts = includeInformation(requestType) && tourId == null && category == null
                ? contactMessageRepository.findAdminRequests(startDateTime, endDateTime)
                : List.of();

        Map<LocalDate, Long> reservationsByDate = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getCreatedAt().toLocalDate(), LinkedHashMap::new, Collectors.counting()));
        Map<LocalDate, Long> informationByDate = contacts.stream()
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().toLocalDate(), LinkedHashMap::new, Collectors.counting()));

        List<AdminTimeSeriesPointDto> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            points.add(AdminTimeSeriesPointDto.builder()
                    .date(day)
                    .reservations(reservationsByDate.getOrDefault(day, 0L))
                    .informationRequests(informationByDate.getOrDefault(day, 0L))
                    .build());
        }
        return points;
    }

    public List<AdminDemandDto> getTopTours(LocalDate startDate, LocalDate endDate, Long tourId, TourCategory category, int limit) {
        List<Booking> bookings = bookingRepository.findAdminRequests(startDateTime(startDate), endDateTime(endDate), tourId, category);
        return topTours(bookings, limit);
    }

    public List<AdminDemandDto> getTopCategories(LocalDate startDate, LocalDate endDate, Long tourId, TourCategory category, int limit) {
        List<Booking> bookings = bookingRepository.findAdminRequests(startDateTime(startDate), endDateTime(endDate), tourId, category);
        return topCategories(bookings, limit);
    }

    public PagedResponse<AdminRequestDto> getRequests(LocalDate startDate, LocalDate endDate, Long tourId, TourCategory category, String requestType, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("createdAt").descending());
        LocalDateTime start = startDateTime(startDate);
        LocalDateTime end = endDateTime(endDate);

        if ("reservation".equalsIgnoreCase(requestType)) {
            Page<Booking> bookingPage = bookingRepository.findAdminRequestsPaged(start, end, tourId, category, pageable);
            return toPaged(bookingPage.map(this::bookingToRequest).getContent(), bookingPage.getNumber(), bookingPage.getSize(), bookingPage.getTotalElements());
        }

        if ("information".equalsIgnoreCase(requestType)) {
            if (tourId != null || category != null) {
                return toPaged(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
            }
            Page<ContactMessage> contactPage = contactMessageRepository.findAdminRequestsPaged(start, end, pageable);
            return toPaged(contactPage.map(this::contactToRequest).getContent(), contactPage.getNumber(), contactPage.getSize(), contactPage.getTotalElements());
        }

        List<AdminRequestDto> combined = new ArrayList<>();
        bookingRepository.findAdminRequests(start, end, tourId, category).forEach(b -> combined.add(bookingToRequest(b)));
        if (tourId == null && category == null) {
            contactMessageRepository.findAdminRequests(start, end).forEach(c -> combined.add(contactToRequest(c)));
        }
        combined.sort(Comparator.comparing(AdminRequestDto::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), combined.size());
        int to = Math.min(from + pageable.getPageSize(), combined.size());
        return toPaged(combined.subList(from, to), pageable.getPageNumber(), pageable.getPageSize(), combined.size());
    }

    @Transactional
    public void deactivateTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));
        tour.setIsActive(false);
        tour.setStatus(TourStatus.CANCELLED);
        tourRepository.save(tour);
    }

    public AdminDeleteImpactDto getDeleteImpact(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_FOUND));
        long bookingReferences = bookingRepository.countByTourId(tour.getId());
        boolean canDelete = bookingReferences == 0;
        return AdminDeleteImpactDto.builder()
                .tourId(tour.getId())
                .bookingReferences(bookingReferences)
                .canPermanentlyDelete(canDelete)
                .message(canDelete
                        ? "This tour has no booking references and can be permanently deleted."
                        : "This tour has booking history and cannot be permanently deleted without losing historical data.")
                .build();
    }

    @Transactional
    public void permanentlyDeleteTour(Long tourId) {
        AdminDeleteImpactDto impact = getDeleteImpact(tourId);
        if (!impact.isCanPermanentlyDelete()) {
            throw new GlobalException(ErrorCodes.TOUR_COULD_NOT_BE_DELETED);
        }
        tourRepository.deleteById(tourId);
    }

    private List<AdminDemandDto> topTours(List<Booking> bookings, int limit) {
        return bookings.stream()
                .filter(b -> b.getTour() != null)
                .collect(Collectors.groupingBy(Booking::getTour, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Tour, Long>comparingByValue().reversed())
                .limit(Math.max(limit, 1))
                .map(entry -> demandForTour(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<AdminDemandDto> topCategories(List<Booking> bookings, int limit) {
        return bookings.stream()
                .map(b -> b.getTour() != null ? b.getTour().getCategory() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(category -> category, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<TourCategory, Long>comparingByValue().reversed())
                .limit(Math.max(limit, 1))
                .map(entry -> AdminDemandDto.builder()
                        .name(entry.getKey().getDisplayName())
                        .category(entry.getKey().name())
                        .reservationRequests(entry.getValue())
                        .informationRequests(0)
                        .totalRequests(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private AdminDemandDto demandForTour(Tour tour, long reservationCount) {
        return AdminDemandDto.builder()
                .id(tour.getId())
                .name(tour.getName())
                .category(tour.getCategory() != null ? tour.getCategory().getDisplayName() : null)
                .destination(tour.getDestination())
                .reservationRequests(reservationCount)
                .informationRequests(0)
                .totalRequests(reservationCount)
                .build();
    }

    private AdminRequestDto bookingToRequest(Booking booking) {
        Tour tour = booking.getTour();
        return AdminRequestDto.builder()
                .id(booking.getId())
                .type("reservation")
                .tourId(tour != null ? tour.getId() : null)
                .tourName(tour != null ? tour.getName() : null)
                .category(tour != null && tour.getCategory() != null ? tour.getCategory().getDisplayName() : null)
                .destination(tour != null ? tour.getDestination() : null)
                .requesterName(booking.getUserName())
                .requesterEmail(booking.getUserEmail())
                .requesterPhone(booking.getUserPhone())
                .numberOfPeople(booking.getNumberOfPeople())
                .status(booking.getStatus() != null ? booking.getStatus().name() : null)
                .message(booking.getUserMessage())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private AdminRequestDto contactToRequest(ContactMessage contact) {
        return AdminRequestDto.builder()
                .id(contact.getId())
                .type("information")
                .requesterName(contact.getName())
                .requesterEmail(contact.getEmail())
                .status(contact.isEmailSent() ? "EMAIL_SENT" : "RECEIVED")
                .message(contact.getSubject() + " - " + contact.getMessage())
                .createdAt(contact.getCreatedAt())
                .build();
    }

    public PagedResponse<AdminContactMessageDto> getContactMessages(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("createdAt").descending());
        Page<ContactMessage> contactPage = contactMessageRepository.findAdminRequestsPaged(null, null, pageable);
        List<AdminContactMessageDto> content = contactPage.map(this::contactToDto).getContent();
        int totalPages = contactPage.getTotalPages();
        return PagedResponse.<AdminContactMessageDto>builder()
                .content(content)
                .page(contactPage.getNumber())
                .size(contactPage.getSize())
                .totalElements(contactPage.getTotalElements())
                .totalPages(totalPages)
                .first(contactPage.isFirst())
                .last(contactPage.isLast())
                .hasNext(contactPage.hasNext())
                .hasPrevious(contactPage.hasPrevious())
                .build();
    }

    private AdminContactMessageDto contactToDto(ContactMessage contact) {
        return AdminContactMessageDto.builder()
                .id(contact.getId())
                .name(contact.getName())
                .email(contact.getEmail())
                .subject(contact.getSubject())
                .message(contact.getMessage())
                .emailSent(contact.isEmailSent())
                .createdAt(contact.getCreatedAt())
                .build();
    }

    private PagedResponse<AdminRequestDto> toPaged(List<AdminRequestDto> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return PagedResponse.<AdminRequestDto>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(totalPages == 0 || page >= totalPages - 1)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }

    private boolean includeReservations(String requestType) {
        return requestType == null || requestType.isBlank() || "all".equalsIgnoreCase(requestType) || "reservation".equalsIgnoreCase(requestType);
    }

    private boolean includeInformation(String requestType) {
        return requestType == null || requestType.isBlank() || "all".equalsIgnoreCase(requestType) || "information".equalsIgnoreCase(requestType);
    }

    private LocalDateTime startDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime endDateTime(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }
}
