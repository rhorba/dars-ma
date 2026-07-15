package ma.darsma.backend.matching;

import java.math.BigDecimal;
import java.util.UUID;

public interface TutorMatchRow {
    UUID getTutorUserId();
    BigDecimal getScore();
}
