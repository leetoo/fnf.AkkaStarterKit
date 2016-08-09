package com.zuehlke.carrera.javapilot.show;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuehlke.carrera.api.channel.RoutingKeyNames;
import com.zuehlke.carrera.api.client.Client;
import com.zuehlke.carrera.api.client.rabbit.RabbitClient;
import com.zuehlke.carrera.api.seralize.JacksonSerializer;
import com.zuehlke.carrera.api.seralize.Serializer;
import com.zuehlke.carrera.connection.SimulatorTowardsPilotApiImpl;
import com.zuehlke.carrera.connection.TowardsPilotApi;
import com.zuehlke.carrera.relayapi.messages.RaceStartMessage;
import com.zuehlke.carrera.relayapi.messages.RaceStopMessage;
import com.zuehlke.carrera.relayapi.messages.SensorEvent;
import com.zuehlke.carrera.relayapi.messages.VelocityMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * reads the data from the file at a given frequency and publishes it to the respective exchange/routing
 */
public class RaceEventDataProducer {

    private static final String rabbitHost = "localhost";
    private static final int FREQUENCY = 120;
    private TowardsPilotApi towardsPilotApi;
    Client client;

    public static void main(String[] args) {

        if ( args.length != 1) {
            exit ("Must provide file name");
        }
        new RaceEventDataProducer().produce ( args [0]);
    }

    private void produce(String filename) {

        try {
            // Read and JSON-deserialize from file
            RaceData data = new ObjectMapper().readValue(new File(filename),RaceData.class);
            System.out.println("Replaying from track " + data.getTrackId() + " for team " + data.getTeamId());

            // Connect to rabbit with our client library as simulator (we're producing readings)
            connectSimulatorWithRabbit(data.getTeamId());

            produce ( data );
        } catch (FileNotFoundException e) {
            exit ( "No such file " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            exit ( "Failed to read from " + filename);
        }
        client.disconnect();
    }

    private void produce(RaceData data) {

        createStartMessage ( data );

        int vs = data.getVelocityMessages().size();
        int ss = data.getSensorEvents().size();
        int vi = 0;
        int si = 0;
        VelocityMessage currentVelocity;
        SensorEvent currentEvent;

        for ( int i = 0; i < vs + ss; i++ ) {
            sleep ( 1000 / FREQUENCY );
            if ( vi < vs  ) {
                currentVelocity = data.getVelocityMessages().get(vi);
            } else {
                if ( si < ss ) {
                    towardsPilotApi.sensor(data.getSensorEvents().get(si++));
                }
                continue;
            }
            if ( si < ss  ) {
                currentEvent = data.getSensorEvents().get(si);
            } else {
                if ( vi < vs ) {
                    towardsPilotApi.velocity(data.getVelocityMessages().get(vi++));
                }
                continue;
            }

            if ( currentVelocity.getT() > currentEvent.getT()) {
                towardsPilotApi.sensor(currentEvent);
                si++;
            } else {
                towardsPilotApi.velocity(currentVelocity);
                vi++;
            }
        }

        createStopMessage(data);
    }

    private void createStartMessage(RaceData data) {
        String track = data.getTrackId();
        String team = data.getTeamId();
        long now = System.currentTimeMillis();
        towardsPilotApi.raceStart(new RaceStartMessage(track, "replay", team, now, "replayed from file", false));
    }

    private void createStopMessage(RaceData data) {
        String track = data.getTrackId();
        String team = data.getTeamId();
        long now = System.currentTimeMillis();
        towardsPilotApi.raceStop(new RaceStopMessage(track, team, now, "replay"));
    }


    private void connectSimulatorWithRabbit ( String teamName ) {

        client = new RabbitClient();
        client.connect(rabbitHost);
        Serializer serializer = new JacksonSerializer();
        towardsPilotApi = new SimulatorTowardsPilotApiImpl(
                client, teamName, new RoutingKeyNames(teamName), serializer);
    }



    private void sleep(int period) {
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
            exit("Got interrupted: " + e.getMessage());
        }
    }

    private static void exit ( String message ) {
        System.out.println(message);
        System.exit(-1);
    }
}
