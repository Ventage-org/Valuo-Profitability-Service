package pe.ventage.valuo.profitability.shared.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Superclase para agregados raíz con soporte de auditoría temporal (createdAt, updatedAt).
 * Por directiva arquitectónica DDD, no registra eventos de integración (no hay message broker).
 *
 * @param <T> Tipo del agregado raíz
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableAbstractAggregateRoot<T> {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AuditableAbstractAggregateRoot() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
