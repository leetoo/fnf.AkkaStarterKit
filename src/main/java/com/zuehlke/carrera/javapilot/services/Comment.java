package com.zuehlke.carrera.javapilot.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Comment {

	private final String text;

	@JsonCreator
	public Comment(@JsonProperty("text") final String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
