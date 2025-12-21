package com.app.replant.entity;

import com.app.replant.entity.type.JobType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "goal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_job")
    private JobType goalJob;

    @Column(name = "goal_start_date")
    private LocalDate goalStartDate;

    @Column(name = "goal_income")
    private String goalIncome;

    @Column(name = "previous_goal_money")
    private Integer previousGoalMoney;
}
