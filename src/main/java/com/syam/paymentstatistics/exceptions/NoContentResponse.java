package com.syam.paymentstatistics.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NO_CONTENT)
public class NoContentResponse extends GenericException {

	private static final long serialVersionUID = 386585074942977143L;

	public NoContentResponse(String errorMessage) {
		super(errorMessage);
	}
}
