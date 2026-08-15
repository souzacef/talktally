package com.talktally.application.category;

public final class CategoryCodeNotFoundException extends RuntimeException {

	public CategoryCodeNotFoundException(String code) {
		super("category code is unavailable: " + code);
	}
}
