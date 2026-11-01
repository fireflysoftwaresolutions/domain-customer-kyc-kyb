package io.fireflysoftware.domain.kyc.kyb.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxKybVerificationAdapterImplTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SandboxKybVerificationAdapterImpl.class);

    @Test
    void missingSelectionMustNotRegisterSyntheticProvider() {
        runner.run(context -> assertThat(context).doesNotHaveBean(KybVerificationPort.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown", "live"})
    void otherSelectionsMustNotRegisterSyntheticProvider(String selection) {
        runner.withPropertyValues("integration.kyb-verification.provider=" + selection)
                .run(context -> assertThat(context).doesNotHaveBean(KybVerificationPort.class));
    }

    @Test
    void explicitSandboxSelectionRegistersSampleWithStableBeanName() {
        runner.withPropertyValues("integration.kyb-verification.provider=sandbox").run(context -> {
            assertThat(context).hasSingleBean(KybVerificationPort.class);
            assertThat(context).hasBean("sandboxKybVerificationAdapter");
        });
    }

    @Test
    void missingProviderRefusesRequiredConsumerStartup() {
        runner.withUserConfiguration(RequiredProviderConfiguration.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class RequiredProviderConfiguration {
        @Bean
        RequiredConsumer requiredConsumer(KybVerificationPort provider) {
            return new RequiredConsumer(provider);
        }
    }

    record RequiredConsumer(KybVerificationPort provider) {}
}
