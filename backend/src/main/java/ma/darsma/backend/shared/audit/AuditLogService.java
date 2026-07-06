package ma.darsma.backend.shared.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogEntryRepository auditLogEntryRepository;

    public AuditLogService(AuditLogEntryRepository auditLogEntryRepository) {
        this.auditLogEntryRepository = auditLogEntryRepository;
    }

    @Transactional
    public AuditLogEntry record(UUID actorId, String action, String targetType, UUID targetId, Map<String, Object> metadata) {
        AuditLogEntry entry = AuditLogEntry.builder()
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .metadata(metadata)
                .build();
        return auditLogEntryRepository.save(entry);
    }
}
