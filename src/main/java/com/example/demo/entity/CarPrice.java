package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "car_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false, unique = true)
    private Car car;

    @Column(name = "price_hour")
    private BigDecimal priceHour;

    @Column(name = "price_day")
    private BigDecimal priceDay;

    @Column(name = "price_month")
    private BigDecimal priceMonth;
}
