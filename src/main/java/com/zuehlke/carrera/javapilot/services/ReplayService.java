package com.zuehlke.carrera.javapilot.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

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
		return new Replay(path.getFileName().toString(), getComments(path.toFile()));
	}

	private List<String> getComments(final File replayDirectory) {
		final File commentsFile = new File(replayDirectory, "comments");
		if (!commentsFile.exists()) {
			return Collections.emptyList();
		}

		try (final Stream<String> lines = Files.lines(commentsFile.toPath())) {
			return lines.collect(Collectors.toList());
		} catch (final IOException e) {
			throw new RuntimeException("Error occured while reading comments " + e);
		}
	}

	public void saveComment(String tag, String comment) {
		final File replay = new File("data/" + tag);
		if (!replay.isDirectory()) {
			throw new RuntimeException("The specified tag does not exist.");
		}

		final File commentFile = new File(replay, "comments");
		try (final FileWriter writer = new FileWriter(commentFile, true)) {
			writer.write(comment + "\n");
		} catch (final IOException e) {
			throw new RuntimeException("Error occured while saving comment " + e);
		}
	}

}
