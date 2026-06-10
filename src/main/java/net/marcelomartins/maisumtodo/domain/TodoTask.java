package net.marcelomartins.maisumtodo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "todo_task", uniqueConstraints = {
        @UniqueConstraint(name = "uk_todo_task_uuid", columnNames = "uuid")
})
public class TodoTask extends EntityBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_project_id", nullable = false)
    public TodoProject project;

    @Column(nullable = false)
    public String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public TaskStatus status = TaskStatus.TODO;

    @Column(nullable = false, precision = 20, scale = 10)
    public BigDecimal sortOrder = BigDecimal.ZERO;
}
