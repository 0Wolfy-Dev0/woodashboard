package com.woodash.event.entity;

import com.woodash.common.BaseEntity;
import com.woodash.group.entity.Group;
import com.woodash.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group sharedGroup;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "all_day", nullable = false)
    private boolean allDay = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_unit", length = 16)
    private RecurrenceUnit recurrenceUnit;

    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval;

    @Column(name = "recurrence_end")
    private LocalDateTime recurrenceEnd;
}
