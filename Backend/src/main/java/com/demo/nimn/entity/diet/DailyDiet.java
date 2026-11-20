package com.demo.nimn.entity.diet;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "t_daily_diet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyDiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @Column(length = 254) // same limit as Users.email (#19)
    private String userEmail;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn
    private List<FoodSelection> foodSelections;
}