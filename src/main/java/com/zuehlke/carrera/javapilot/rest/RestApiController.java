package com.zuehlke.carrera.javapilot.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zuehlke.carrera.javapilot.services.Comment;
import com.zuehlke.carrera.javapilot.services.PilotService;
import com.zuehlke.carrera.javapilot.services.Replay;
import com.zuehlke.carrera.javapilot.services.ReplayService;

@RestController
@RequestMapping("/api/")
public class RestApiController {

	@Autowired
	public PilotService pilotService;

	@Autowired
	public ReplayService replayService;

	@RequestMapping(value = "/replay/{tag}", method = RequestMethod.GET, produces = "application/json")
	public String replay(@PathVariable String tag) {

		pilotService.replay(tag);
		return "ok";
	}

	@RequestMapping(value = "/replay", method = RequestMethod.GET, produces = "application/json")
	public List<Replay> getReplays() {
		return replayService.getReplays();
	}

	@RequestMapping(value = "/replay/{tag}/comment", method = RequestMethod.POST, produces = "application/json")
	public void postReplayComment(@PathVariable String tag, @RequestBody Comment comment) {
		replayService.saveComment(tag, comment.getText());
	}

}
