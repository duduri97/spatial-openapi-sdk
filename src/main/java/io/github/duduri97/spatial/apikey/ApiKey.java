package io.github.duduri97.spatial.apikey;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_keys_key_value", columnList = "keyValue", unique = true)
})
public class ApiKey {

    public enum Status {
        PENDING, APPROVED, REVOKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String keyValue;

    @Column(nullable = false, length = 100)
    private String applicationName;

    @Column(nullable = false, length = 100)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private int dailyQuota;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant approvedAt;

    protected ApiKey() {
    }

    public ApiKey(String keyValue, String applicationName, String ownerName, Status status, int dailyQuota) {
        this.keyValue = keyValue;
        this.applicationName = applicationName;
        this.ownerName = ownerName;
        this.status = status;
        this.dailyQuota = dailyQuota;
        this.createdAt = Instant.now();
        if (status == Status.APPROVED) {
            this.approvedAt = this.createdAt;
        }
    }

    public void approve() {
        this.status = Status.APPROVED;
        this.approvedAt = Instant.now();
    }

    public void revoke() {
        this.status = Status.REVOKED;
    }

    public Long getId() { return id; }
    public String getKeyValue() { return keyValue; }
    public String getApplicationName() { return applicationName; }
    public String getOwnerName() { return ownerName; }
    public Status getStatus() { return status; }
    public int getDailyQuota() { return dailyQuota; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getApprovedAt() { return approvedAt; }
}
