package com.example.wedding.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "wedding_events")
public class WeddingEvent {
    @Id
    @Column(name = "wedding_event_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "wedding_id", nullable = false)
    private WeddingCard wedding;

    @Size(max = 255)
    @Column(name = "invite_text")
    private String inviteText;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_time")
    private LocalTime eventTime;

    @Size(max = 255)
    @Column(name = "venue_name")
    private String venueName;

    @Size(max = 500)
    @Column(name = "address", length = 500)
    private String address;

    @Lob
    @Column(name = "link_map")
    private String linkMap;

}