package com.ticketstorm.ai.domain.port;

import com.ticketstorm.shared.common.dto.FraudDTO;

public interface FraudDetectionPort {
    FraudDTO.FraudCheckRequest buildRequest(String userId, String reservationId,
                                             String eventId, String ipAddress,
                                             String deviceFingerprint, java.math.BigDecimal amount);
}
