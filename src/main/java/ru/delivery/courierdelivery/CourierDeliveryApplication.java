package ru.delivery.courierdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.delivery.courierdelivery.security.jwt.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class CourierDeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(CourierDeliveryApplication.class, args);
	}
}
