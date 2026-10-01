package com.odontocare.installation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "installation_profile")
public class InstallationProfile {
    @Id
    private Short id;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "time_zone", nullable = false, length = 60)
    private String timeZone;

    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InstallationProfile() { }

    public String getDisplayName() { return displayName; }
    public String getTimeZone() { return timeZone; }
    public String getCurrency() { return currency; }
}
