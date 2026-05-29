package com.interviewassistant.service;

import com.interviewassistant.dto.UpdateProfileRequest;
import com.interviewassistant.dto.UserProfileResponse;

public interface UserService {
    UserProfileResponse getProfile(Long userId);
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
}
