package com.atlas.core.config;

import com.atlas.core.model.ModelProfile;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "atlas.models")
public record ModelProperties(String defaultProfile, Map<String, ModelProfile> profiles) {

    public ModelProfile get(String profileName) {
        ModelProfile profile = profiles.get(profileName);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown model profile: " + profileName);
        }
        return profile;
    }

    public ModelProfile getDefault() {
        return get(defaultProfile);
    }
}
