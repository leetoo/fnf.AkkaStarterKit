package com.zuehlke.carrera.javapilot.services;

import java.util.List;

public class Replay {

	private final String tag;
	private final List<String> comments;

	public Replay(final String tag, final List<String> comments) {
		this.tag = tag;
		this.comments = comments;
	}

	public String getTag() {
		return tag;
	}

	public List<String> getComments() {
		return comments;
	}

}
