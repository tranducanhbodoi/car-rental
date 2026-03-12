package com.example.demo.repository;

import com.example.demo.entity.CarPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CarPriceRepository extends JpaRepository<CarPrice, Integer> {
    Optional<CarPrice> findByCarCarId(Integer carId);
}
