package ma.darsma.backend.admin;

import ma.darsma.backend.admin.dto.AdminAnalyticsResponse;
import ma.darsma.backend.auth.Role;
import ma.darsma.backend.auth.UserRepository;
import ma.darsma.backend.booking.BookingRepository;
import ma.darsma.backend.booking.BookingStatus;
import ma.darsma.backend.gig.GigRequestRepository;
import ma.darsma.backend.matching.MatchSuggestionRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final GigRequestRepository gigRequestRepository;
    private final MatchSuggestionRepository matchSuggestionRepository;

    public AdminAnalyticsService(UserRepository userRepository, BookingRepository bookingRepository,
                                  GigRequestRepository gigRequestRepository,
                                  MatchSuggestionRepository matchSuggestionRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.gigRequestRepository = gigRequestRepository;
        this.matchSuggestionRepository = matchSuggestionRepository;
    }

    public AdminAnalyticsResponse compute() {
        long totalGigs = gigRequestRepository.count();
        long matchedGigs = matchSuggestionRepository.countDistinctGigRequestId();
        double matchRatePercent = totalGigs == 0 ? 0.0 : (matchedGigs * 100.0) / totalGigs;

        return new AdminAnalyticsResponse(
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.TUTOR),
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.COMPLETED),
                bookingRepository.sumAgreedPriceMadByStatus(BookingStatus.COMPLETED),
                matchRatePercent
        );
    }
}
