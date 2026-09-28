package com.ticketstorm.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "payload")
public class InventoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String seatId;

    @Column(nullable = false)
    private String sectionId;

    @Column(nullable = false)
    private String eventType;

    private String userId;

    private BigDecimal price;

    private String currency;

    @Column(nullable = false)
    private Instant timestamp;

    @Lob
    private String payload;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
