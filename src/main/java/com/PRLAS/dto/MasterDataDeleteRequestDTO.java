package com.PRLAS.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MasterDataDeleteRequestDTO {

    @NotNull(message = "fromDate is required")
    private LocalDateTime fromDate;

    @NotNull(message = "toDate is required")
    private LocalDateTime toDate;

    @NotBlank(message = "password is required")
    private String password;
}
