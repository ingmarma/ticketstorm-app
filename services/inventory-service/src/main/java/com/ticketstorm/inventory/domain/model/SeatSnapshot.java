package com.ticketstorm.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SeatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String seatId;

    @Column(nullable = false)
    private String sectionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    private String currentReservationId;

    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public enum SeatStatus {
        AVAILABLE, BLOCKED, RESERVED, CONFIRMED, RELEASED
    }
}
