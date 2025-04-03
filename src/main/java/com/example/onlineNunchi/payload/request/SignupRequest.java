package com.example.onlineNunchi.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignupRequest {


    @NotBlank
    private String email;

    @NotBlank
    @Size(min=6 , max=20)
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String department;

}
