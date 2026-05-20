package com.goylik.auth_service.security.filter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.internal.api-keys")
@Getter @Setter
public class InternalApiKeysProperties {
    private List<String> trustedServices = new ArrayList<>();
}
