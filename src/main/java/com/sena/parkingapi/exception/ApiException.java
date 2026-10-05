package com.sena.parkingapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatus status;

	public ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public static ApiException notFound(String msg) {
		return new ApiException(HttpStatus.NOT_FOUND, msg);
	}

	public static ApiException conflict(String msg) {
		return new ApiException(HttpStatus.CONFLICT, msg);
	}

	public static ApiException badRequest(String msg) {
		return new ApiException(HttpStatus.BAD_REQUEST, msg);
	}
}
