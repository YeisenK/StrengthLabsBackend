package com.strengthlabs.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines")
public class RoutineJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_es")
    private String nameEs;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private String goal;

    @Column(name = "days_per_week", nullable = false)
    private int daysPerWeek;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_es", columnDefinition = "TEXT")
    private String descriptionEs;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<RoutineDayJpaEntity> days = new ArrayList<>();

    protected RoutineJpaEntity() {}

    public RoutineJpaEntity(String id, String name, String nameEs, String level, String goal,
                             int daysPerWeek, String description, String descriptionEs) {
        this.id = id;
        this.name = name;
        this.nameEs = nameEs;
        this.level = level;
        this.goal = goal;
        this.daysPerWeek = daysPerWeek;
        this.description = description;
        this.descriptionEs = descriptionEs;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getNameEs() { return nameEs; }
    public String getLevel() { return level; }
    public String getGoal() { return goal; }
    public int getDaysPerWeek() { return daysPerWeek; }
    public String getDescription() { return description; }
    public String getDescriptionEs() { return descriptionEs; }
    public List<RoutineDayJpaEntity> getDays() { return days; }
    public void addDay(RoutineDayJpaEntity day) { days.add(day); }
}
