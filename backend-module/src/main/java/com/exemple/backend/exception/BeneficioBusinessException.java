package com.exemple.backend.exception;

public class BeneficioBusinessException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6576215533087482243L;

	public BeneficioBusinessException(String message) {
        super(message);
    }
    
    public BeneficioBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}