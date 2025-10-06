package com.example.memorygame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MemorygameApplication {
	public static void main(String[] args) {
		SpringApplication.run(MemorygameApplication.class, args);
	}
}
