package ru.delivery.courierdelivery.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken
) {
}
