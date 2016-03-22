package com.zuehlke.carrera.javapilot.services;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {

	private final List<Comment> comments;
	private final List<Tag> tags;

	@JsonCreator
	public Metadata(@JsonProperty("comments") List<Comment> comments, @JsonProperty("tags") List<Tag> tags) {
		this.comments = comments;
		this.tags = tags;
	}

	public static Metadata empty() {
		return new Metadata(new ArrayList<>(), new ArrayList<>());
	}

	public List<Comment> getComments() {
		return comments;
	}

	public List<Tag> getTags() {
		return tags;
	}

}
