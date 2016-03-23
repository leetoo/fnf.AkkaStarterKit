package com.zuehlke.carrera.javapilot.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ReplayService {

	public List<Replay> getReplays() {
		final File dataDirectory = new File("data");

		try (final Stream<Path> replays = Files.list(dataDirectory.toPath())) {
			return replays.map(this::mapReplay).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException("Could not fetch replays from data directory. Original error: " + e);
		}
	}

	private Replay mapReplay(Path path) {
		final String replayTagName = path.getFileName().toString();

		return new Replay(replayTagName, getCreationDateTime(path),
				getMetadata(new File(path.toFile(), "metadata.json"), replayTagName));
	}

	public void saveComment(final String replayTag, final String comment) {
		final File replay = getReplayDirectory(replayTag);
		final File metadataFile = new File(replay, "metadata.json");
		final Metadata metadata = getMetadata(metadataFile, replayTag);

		metadata.getComments().add(new Comment(comment));
		final Metadata updatedMetadata = new Metadata(metadata.getComments(), metadata.getTags());
		writeMetadata(updatedMetadata, metadataFile, replayTag);
	}

	public void saveTags(final String replayTag, final List<Tag> tags) {
		final File replay = getReplayDirectory(replayTag);
		final File metadataFile = new File(replay, "metadata.json");
		final Metadata metadata = getMetadata(metadataFile, replayTag);

		final Metadata updatedMetadata = new Metadata(metadata.getComments(), tags);

		writeMetadata(updatedMetadata, metadataFile, replayTag);
	}

	private void writeMetadata(final Metadata updatedMetadata, final File metadataFile, final String replayTag) {
		try {
			if (!metadataFile.exists()) {
				Files.createFile(metadataFile.toPath());
			}
			new ObjectMapper().writeValue(metadataFile, updatedMetadata);
		} catch (IOException e) {
			throw new RuntimeException("Error occured while writing metadata for replay with tag: " + replayTag, e);
		}
	}

	private Metadata getMetadata(final File metadataFile, final String replayTag) {
		if (!metadataFile.exists()) {
			return Metadata.empty();
		}

		try {
			return new ObjectMapper().readValue(metadataFile, Metadata.class);
		} catch (IOException e) {
			throw new RuntimeException("Error occured while reading metadata for replay with tag: " + replayTag, e);
		}
	}

	private LocalDateTime getCreationDateTime(final Path path) {
		try {
			final BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
			return LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
		} catch (IOException e) {
			throw new RuntimeException("Failed to fetch replays from data directory. Original error: " + e);
		}
	}

	private File getReplayDirectory(final String tag) {
		final File replay = new File("data/" + tag);
		if (!replay.isDirectory()) {
			throw new RuntimeException("The specified tag does not exist.");
		}
		return replay;
	}

}
