package io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.adapters;

import io.fireflysoftware.domain.kyc.kyb.core.kyb.ports.KybProviderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxKybProviderAdapterImplTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SandboxKybProviderAdapterImpl.class);

    @Test
    void missingSelectionMustNotRegisterSyntheticProvider() {
        runner.run(context -> assertThat(context).doesNotHaveBean(KybProviderPort.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown", "live"})
    void otherSelectionsMustNotRegisterSyntheticProvider(String selection) {
        runner.withPropertyValues("kyb.provider=" + selection)
                .run(context -> assertThat(context).doesNotHaveBean(KybProviderPort.class));
    }

    @Test
    void explicitSandboxSelectionRegistersSampleWithStableBeanName() {
        runner.withPropertyValues("kyb.provider=sandbox").run(context -> {
            assertThat(context).hasSingleBean(KybProviderPort.class);
            assertThat(context).hasBean("sandboxKybProviderAdapter");
        });
    }

    @Test
    void missingProviderRefusesRequiredConsumerStartup() {
        runner.withUserConfiguration(RequiredConsumer.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
        });
    }

    record RequiredConsumer(KybProviderPort provider) {}
}
