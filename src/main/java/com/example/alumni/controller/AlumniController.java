package com.example.alumni.controller;

import java.util.List;

import com.example.alumni.dto.AlumniProfileDto;
import com.example.alumni.dto.AlumniSearchRequest;
import com.example.alumni.dto.ApiResponse;
import com.example.alumni.service.AlumniService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
public class AlumniController {

	private final AlumniService service;

	@PostMapping("/search")
	public ApiResponse<List<AlumniProfileDto>> search(@Valid @RequestBody AlumniSearchRequest request) {
		return ApiResponse.success(service.search(request));
	}

	@GetMapping("/all")
	public ApiResponse<List<AlumniProfileDto>> all() {
		return ApiResponse.success(service.findAll());
	}
}
