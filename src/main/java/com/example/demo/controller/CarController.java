package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping("/search")
    public ResponseEntity<List<CarResponse>> searchCars(CarSearchRequest request) {
        return ResponseEntity.ok(carService.searchCars(request));
    }

    @GetMapping
    public ResponseEntity<List<CarResponse>> getAllCars() {
        return ResponseEntity.ok(carService.getAllCars());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarResponse> getCarById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(carService.getCarById(id));
    }

    @PostMapping
    public ResponseEntity<CarResponse> createCar(@Valid @RequestBody CarRequest request, Authentication auth) {
        return ResponseEntity.ok(carService.createCar(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponse> updateCar(@PathVariable("id") Integer id,
                                                  @Valid @RequestBody CarRequest request,
                                                  Authentication auth) {
        return ResponseEntity.ok(carService.updateCar(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable("id") Integer id, Authentication auth) {
        carService.deleteCar(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
