package net.marcelomartins.maisumtodo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "system_login", uniqueConstraints = {
        @UniqueConstraint(name = "uk_system_login_uuid", columnNames = "uuid"),
        @UniqueConstraint(name = "uk_system_login_email", columnNames = "email")
})
public class SystemLogin extends EntityBase {

    @Column(nullable = false)
    public String email;

    @JsonIgnore
    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public Long authTokenVersion = 0L;

    public long currentAuthTokenVersion() {
        return authTokenVersion == null ? 0L : authTokenVersion;
    }

    public void revokeAuthTokens() {
        authTokenVersion = currentAuthTokenVersion() + 1L;
    }
}
