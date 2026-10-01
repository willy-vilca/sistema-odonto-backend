package com.odontocare.installation.repository;

import com.odontocare.installation.model.InstallationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationProfileRepository extends JpaRepository<InstallationProfile, Short> { }
