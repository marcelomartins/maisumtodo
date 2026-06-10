package net.marcelomartins.maisumtodo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "todo_project", uniqueConstraints = {
        @UniqueConstraint(name = "uk_todo_project_uuid", columnNames = "uuid")
})
public class TodoProject extends EntityBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_login_id", nullable = false)
    public SystemLogin systemLogin;

    @Column(nullable = false)
    public String name;
}
