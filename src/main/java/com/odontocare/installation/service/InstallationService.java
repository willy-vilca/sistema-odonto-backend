package com.odontocare.installation.service;

import com.odontocare.installation.dto.InstallationResponse;
import com.odontocare.installation.repository.InstallationProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstallationService {
    private static final short SINGLE_INSTALLATION_ID = 1;
    private final InstallationProfileRepository repository;

    public InstallationService(InstallationProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true, timeout = 5)
    public InstallationResponse getInstallation() {
        var profile = repository.findById(SINGLE_INSTALLATION_ID)
                .orElseThrow(() -> new IllegalStateException("Installation profile is missing"));
        return new InstallationResponse(profile.getDisplayName(), profile.getTimeZone(), profile.getCurrency());
    }
}
