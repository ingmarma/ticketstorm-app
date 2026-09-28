package com.ticketstorm.catalog.domain.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String category;

    @NotBlank
    @Column(nullable = false)
    private String venue;

    @NotBlank
    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Instant eventDate;

    private Instant saleStart;

    private Instant saleEnd;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal minPrice;

    @Column(nullable = false, length = 3)
    private String currency = "PYG";

    @Column(nullable = false)
    private int totalSeats;

    @Column(nullable = false)
    private int availableSeats;

    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @JsonManagedReference
    private List<TicketSection> ticketSections = new ArrayList<>();

    protected Event() {}

    public Event(String name, String description, String category, String venue, String city,
                 Instant eventDate, Instant saleStart, Instant saleEnd,
                 BigDecimal minPrice, String currency, int totalSeats, String imageUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.venue = venue;
        this.city = city;
        this.eventDate = eventDate;
        this.saleStart = saleStart;
        this.saleEnd = saleEnd;
        this.minPrice = minPrice;
        this.currency = currency;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.imageUrl = imageUrl;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Instant getEventDate() { return eventDate; }
    public void setEventDate(Instant eventDate) { this.eventDate = eventDate; }

    public Instant getSaleStart() { return saleStart; }
    public void setSaleStart(Instant saleStart) { this.saleStart = saleStart; }

    public Instant getSaleEnd() { return saleEnd; }
    public void setSaleEnd(Instant saleEnd) { this.saleEnd = saleEnd; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<TicketSection> getTicketSections() { return ticketSections; }
    public void setTicketSections(List<TicketSection> ticketSections) { this.ticketSections = ticketSections; }
}
