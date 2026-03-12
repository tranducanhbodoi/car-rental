package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "brands")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    private Integer brandId;

    @Column(nullable = false)
    private String name;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "brand")
    private List<Car> cars;
}
