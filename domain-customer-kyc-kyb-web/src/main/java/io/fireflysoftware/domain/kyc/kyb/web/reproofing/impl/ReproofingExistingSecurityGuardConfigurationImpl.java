package io.fireflysoftware.domain.kyc.kyb.web.reproofing.impl;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.ClassUtils;

@Configuration(proxyBeanMethods=false)
public class ReproofingExistingSecurityGuardConfigurationImpl {
    @Bean public SmartInitializingSingleton reproofingExistingSecurityGuard(Environment env,ConfigurableListableBeanFactory beans){
        return ()->{
            if(!ClassUtils.isPresent("org.fireflyframework.security.rs.ResourceServerAutoConfiguration",getClass().getClassLoader())
                    || !env.getProperty("firefly.security.resource-server.enabled",Boolean.class,true)) return;
            String name=env.getProperty("firefly.reproofing.existing-security-chain-bean","fireflySecurityWebFilterChain");
            if(!beans.containsBean(name) || !beans.isTypeMatch(name,SecurityWebFilterChain.class)
                    || beans.getBean(name)==beans.getBean("reproofingBoundary"))
                throw new IllegalStateException("Independent existing application security chain required");
        };
    }
}
