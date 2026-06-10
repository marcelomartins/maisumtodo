package net.marcelomartins.maisumtodo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class EntityBase extends PanacheEntityBase {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 36)
    public String uuid;

    @JsonIgnore
    @Column(nullable = false)
    public LocalDateTime dateCreated;

    @JsonIgnore
    @Column(nullable = false)
    public LocalDateTime lastUpdated;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (uuid == null || uuid.isBlank()) {
            uuid = UUID.randomUUID().toString();
        }
        if (dateCreated == null) {
            dateCreated = now;
        }
        lastUpdated = now;
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
