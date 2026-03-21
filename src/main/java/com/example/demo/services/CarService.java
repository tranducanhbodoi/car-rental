package com.example.demo.services;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CarPriceRepository carPriceRepository;
    private final CarImageRepository carImageRepository;
    private final CarScheduleRepository carScheduleRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public List<CarResponse> searchCars(CarSearchRequest request) {
        LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now();
        LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now().plusYears(1);

        List<Car> cars = carRepository.searchAvailableCars(
                request.getCity(), request.getBrandId(), request.getCategoryId(),
                startDate, endDate
        );

        return cars.stream().map(this::mapToCarResponse).collect(Collectors.toList());
    }

    public CarResponse getCarById(Integer carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + carId));
        return mapToCarResponse(car);
    }

    public List<CarResponse> getAllCars() {
        return carRepository.findAll().stream()
                .map(this::mapToCarResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CarResponse createCar(CarRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (carRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new RuntimeException("Biển số xe đã được đăng ký trong hệ thống");
        }

        Car car = Car.builder()
                .owner(owner)
                .brand(brand)
                .category(category)
                .name(request.getName())
                .color(request.getColor())
                .year(request.getYear())
                .licensePlate(request.getLicensePlate())
                .description(request.getDescription())
                .locationCity(request.getLocationCity())
                .locationDistrict(request.getLocationDistrict())
                .build();

        car = carRepository.save(car);

        // Save price
        if (request.getPriceDay() != null || request.getPriceHour() != null || request.getPriceMonth() != null) {
            CarPrice price = CarPrice.builder()
                    .car(car)
                    .priceHour(request.getPriceHour())
                    .priceDay(request.getPriceDay())
                    .priceMonth(request.getPriceMonth())
                    .build();
            carPriceRepository.save(price);
            car.setCarPrice(price);
        }

        // Save images
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<CarImage> images = new ArrayList<>();
            for (String url : request.getImageUrls()) {
                CarImage image = CarImage.builder().car(car).imageUrl(url).build();
                images.add(carImageRepository.save(image));
            }
            car.setCarImages(images);
        }

        return mapToCarResponse(car);
    }

    @Transactional
    public CarResponse updateCar(Integer carId, CarRequest request, String ownerEmail) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));

        if (!car.getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("You are not the owner of this car");
        }

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (carRepository.existsByLicensePlateAndCarIdNot(request.getLicensePlate(), carId)) {
            throw new RuntimeException("Biển số xe đã được đăng ký bởi hệ thống cho xe khác");
        }

        car.setBrand(brand);
        car.setCategory(category);
        car.setName(request.getName());
        car.setColor(request.getColor());
        car.setYear(request.getYear());
        car.setLicensePlate(request.getLicensePlate());
        car.setDescription(request.getDescription());
        car.setLocationCity(request.getLocationCity());
        car.setLocationDistrict(request.getLocationDistrict());

        carRepository.save(car);

        // Update price
        CarPrice price = car.getCarPrice();
        if (price == null) {
            price = CarPrice.builder().car(car).build();
        }
        price.setPriceHour(request.getPriceHour());
        price.setPriceDay(request.getPriceDay());
        price.setPriceMonth(request.getPriceMonth());
        carPriceRepository.save(price);

        // Update images
        if (request.getImageUrls() != null) {
            // Fix: Clear and add to the existing collection instead of replacing it
            // This avoids "A collection with orphan deletion was no longer referenced" error
            car.getCarImages().clear();
            for (String url : request.getImageUrls()) {
                car.getCarImages().add(CarImage.builder().car(car).imageUrl(url).build());
            }
        }

        return mapToCarResponse(car);
    }

    public void deleteCar(Integer carId, String ownerEmail) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));
        if (!car.getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("You are not the owner of this car");
        }

        boolean hasActiveBookings = bookingRepository.existsByCarCarIdAndStatusIn(carId, List.of("PENDING_PAYMENT", "PENDING", "CONFIRMED", "IN_PROGRESS"));
        if (hasActiveBookings) {
            throw new RuntimeException("Không thể xóa xe khi đang có đơn đặt xe (chưa hoàn thành)");
        }

        carRepository.delete(car);
    }

    public List<CarResponse> getCarsByOwner(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));
        return carRepository.findByOwner(owner).stream()
                .map(this::mapToCarResponse)
                .collect(Collectors.toList());
    }

    private CarResponse mapToCarResponse(Car car) {
        List<String> imageUrls = car.getCarImages() != null
                ? car.getCarImages().stream().map(CarImage::getImageUrl).collect(Collectors.toList())
                : new ArrayList<>();

        List<Feedback> feedbacks = feedbackRepository.findByCarCarId(car.getCarId());
        Double avgRating = feedbacks.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);

        return CarResponse.builder()
                .carId(car.getCarId())
                .name(car.getName())
                .brandName(car.getBrand().getName())
                .brandId(car.getBrand().getBrandId())
                .categoryName(car.getCategory().getName())
                .categoryId(car.getCategory().getCategoryId())
                .ownerName(car.getOwner().getFullName())
                .ownerId(car.getOwner().getId())
                .color(car.getColor())
                .year(car.getYear())
                .licensePlate(car.getLicensePlate())
                .description(car.getDescription())
                .locationCity(car.getLocationCity())
                .locationDistrict(car.getLocationDistrict())
                .status(car.getStatus())
                .createdAt(car.getCreatedAt())
                .priceHour(car.getCarPrice() != null ? car.getCarPrice().getPriceHour() : null)
                .priceDay(car.getCarPrice() != null ? car.getCarPrice().getPriceDay() : null)
                .priceMonth(car.getCarPrice() != null ? car.getCarPrice().getPriceMonth() : null)
                .imageUrls(imageUrls)
                .averageRating(avgRating)
                .totalFeedbacks(feedbacks.size())
                .build();
    }
}
