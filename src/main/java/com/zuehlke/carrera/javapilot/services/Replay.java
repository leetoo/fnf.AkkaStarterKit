package com.zuehlke.carrera.javapilot.services;

import java.util.List;

public class Replay {

	private final String tag;
	private final List<Comment> comments;

	public Replay(final String tag, final List<Comment> comments) {
		this.tag = tag;
		this.comments = comments;
	}

	public String getTag() {
		return tag;
	}

	public List<Comment> getComments() {
		return comments;
	}

}
