package org.dspace.validator;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DSONotFoundException extends RuntimeException {

	public DSONotFoundException(String itemUUIDorId) {
		super("could not find item '" + itemUUIDorId + "'.");
	}
	
}
