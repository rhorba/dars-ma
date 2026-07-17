package ma.darsma.backend.admin;

import ma.darsma.backend.admin.dto.AdminAnalyticsResponse;
import ma.darsma.backend.auth.Role;
import ma.darsma.backend.auth.UserRepository;
import ma.darsma.backend.booking.BookingRepository;
import ma.darsma.backend.booking.BookingStatus;
import ma.darsma.backend.gig.GigRequestRepository;
import ma.darsma.backend.matching.MatchSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAnalyticsServiceTest {

    private UserRepository userRepository;
    private BookingRepository bookingRepository;
    private GigRequestRepository gigRequestRepository;
    private MatchSuggestionRepository matchSuggestionRepository;
    private AdminAnalyticsService adminAnalyticsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        bookingRepository = mock(BookingRepository.class);
        gigRequestRepository = mock(GigRequestRepository.class);
        matchSuggestionRepository = mock(MatchSuggestionRepository.class);
        adminAnalyticsService = new AdminAnalyticsService(
                userRepository, bookingRepository, gigRequestRepository, matchSuggestionRepository);
    }

    @Test
    void compute_aggregatesAllMetrics() {
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(10L);
        when(userRepository.countByRole(Role.TUTOR)).thenReturn(4L);
        when(bookingRepository.count()).thenReturn(20L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(12L);
        when(bookingRepository.sumAgreedPriceMadByStatus(BookingStatus.COMPLETED)).thenReturn(new BigDecimal("2400.00"));
        when(gigRequestRepository.count()).thenReturn(25L);
        when(matchSuggestionRepository.countDistinctGigRequestId()).thenReturn(20L);

        AdminAnalyticsResponse result = adminAnalyticsService.compute();

        assertThat(result.studentSignups()).isEqualTo(10L);
        assertThat(result.tutorSignups()).isEqualTo(4L);
        assertThat(result.totalBookings()).isEqualTo(20L);
        assertThat(result.completedBookings()).isEqualTo(12L);
        assertThat(result.gmvMad()).isEqualByComparingTo("2400.00");
        assertThat(result.matchRatePercent()).isEqualTo(80.0);
    }

    @Test
    void compute_noGigsYet_matchRateIsZeroNotDivisionByZero() {
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(0L);
        when(userRepository.countByRole(Role.TUTOR)).thenReturn(0L);
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(0L);
        when(bookingRepository.sumAgreedPriceMadByStatus(BookingStatus.COMPLETED)).thenReturn(BigDecimal.ZERO);
        when(gigRequestRepository.count()).thenReturn(0L);
        when(matchSuggestionRepository.countDistinctGigRequestId()).thenReturn(0L);

        AdminAnalyticsResponse result = adminAnalyticsService.compute();

        assertThat(result.matchRatePercent()).isEqualTo(0.0);
    }
}
