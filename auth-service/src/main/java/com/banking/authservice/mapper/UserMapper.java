package com.banking.authservice.mapper;


import com.banking.authservice.dto.UserUpdateResponse;
import com.banking.authservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserUpdateResponse toUserUpdateResponse(User user);
}
