package com.syam.paymentstatistics.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends GenericException {

	private static final long serialVersionUID = 7537373315626673345L;

	public BadRequestException(String errorMessage) {
		super(errorMessage);
	}
}
