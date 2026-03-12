package com.example.demo.repository;

import com.example.demo.entity.CarImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarImageRepository extends JpaRepository<CarImage, Integer> {
    List<CarImage> findByCarCarId(Integer carId);
}
