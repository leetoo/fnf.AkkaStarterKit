package com.zuehlke.carrera.javapilot.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Tag {

	private final String name;

	@JsonCreator
	public Tag(@JsonProperty("name") final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
