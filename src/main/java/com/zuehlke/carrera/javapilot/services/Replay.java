package com.zuehlke.carrera.javapilot.services;

import java.time.LocalDateTime;

public class Replay {

	private final String tag;
	private final LocalDateTime creationDate;
	private final Metadata metadata;

	public Replay(final String tag, final LocalDateTime creationDate, final Metadata metadata) {
		this.tag = tag;
		this.creationDate = creationDate;
		this.metadata = metadata;
	}

	public String getTag() {
		return tag;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public Metadata getMetadata() {
		return metadata;
	}

}
