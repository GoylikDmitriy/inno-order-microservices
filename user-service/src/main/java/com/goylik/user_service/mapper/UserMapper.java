package com.goylik.user_service.mapper;

import com.goylik.user_service.model.dto.request.CreateUserRequest;
import com.goylik.user_service.model.dto.request.UpdateUserRequest;
import com.goylik.user_service.model.dto.response.UserResponse;
import com.goylik.user_service.model.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(UpdateUserRequest request, @MappingTarget User user);
}
