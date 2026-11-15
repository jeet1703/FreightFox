package com.example.alumni.exception;

public class PhantomBusterException extends RuntimeException {

	public PhantomBusterException(String message) {
		super(message);
	}

	public PhantomBusterException(String message, Throwable cause) {
		super(message, cause);
	}
}
