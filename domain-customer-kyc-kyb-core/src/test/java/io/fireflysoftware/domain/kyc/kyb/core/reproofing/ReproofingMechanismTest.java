package io.fireflysoftware.domain.kyc.kyb.core.reproofing;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReproofingMechanismTest {
    @Test
    void independentRecoveryOrchestratorIsAvailable() {
        assertThat(getClass().getResource("/io/fireflysoftware/domain/kyc/kyb/core/reproofing/impl/ReproofingServiceImpl.class"))
                .as("credential loss requires an independent provider-backed orchestration mechanism")
                .isNotNull();
    }
}
