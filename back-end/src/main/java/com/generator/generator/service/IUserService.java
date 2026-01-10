package com.generator.generator.service;

import com.generator.generator.dto.AuthResponse;
import com.generator.generator.dto.LoginRequest;
import com.generator.generator.dto.RegisterRequest;
import com.generator.generator.dto.UserResponse;

import java.util.List;

public interface IUserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser(String username);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, RegisterRequest request);
    void deleteUser(Long id);
    void logout();
}

