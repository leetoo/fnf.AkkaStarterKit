package com.zuehlke.carrera.javapilot.show;

import com.zuehlke.carrera.relayapi.messages.PowerControl;
import com.zuehlke.carrera.relayapi.messages.SensorEvent;
import com.zuehlke.carrera.relayapi.messages.VelocityMessage;

import java.util.ArrayList;
import java.util.List;

public class RaceData {

    private String id;
    private long startTime;
    private String teamId;
    private String trackId;
    private String raceType;

    private List<SensorEvent> sensorEvents = new ArrayList<>();
    private List<PowerControl> powerControls = new ArrayList<>();
    private List<VelocityMessage> velocityMessages = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getRaceType() {
        return raceType;
    }

    public void setRaceType(String raceType) {
        this.raceType = raceType;
    }

    public List<SensorEvent> getSensorEvents() {
        return sensorEvents;
    }

    public void setSensorEvents(List<SensorEvent> sensorEvents) {
        this.sensorEvents = sensorEvents;
    }

    public List<PowerControl> getPowerControls() {
        return powerControls;
    }

    public void setPowerControls(List<PowerControl> powerControls) {
        this.powerControls = powerControls;
    }

    public List<VelocityMessage> getVelocityMessages() {
        return velocityMessages;
    }

    public void setVelocityMessages(List<VelocityMessage> velocityMessages) {
        this.velocityMessages = velocityMessages;
    }
}