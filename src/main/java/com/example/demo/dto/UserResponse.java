package com.example.demo.dto;

import com.example.demo.entity.Role;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Integer id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String status;
    private String city;
    private String district;
    private String detailedAddress;
    private LocalDateTime createdAt;
}
