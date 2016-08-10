package com.zuehlke.carrera.javapilot;


import akka.actor.ActorRef;
import com.zuehlke.carrera.api.DirectExchangePilotApiImpl;
import com.zuehlke.carrera.api.PilotApi;
import com.zuehlke.carrera.api.channel.PilotToRelayChannelNames;
import com.zuehlke.carrera.api.channel.RoutingKeyNames;
import com.zuehlke.carrera.api.client.Client;
import com.zuehlke.carrera.api.client.rabbit.RabbitClient;
import com.zuehlke.carrera.api.seralize.JacksonSerializer;
import com.zuehlke.carrera.api.seralize.Serializer;
import com.zuehlke.carrera.connection.*;
import com.zuehlke.carrera.javapilot.config.PilotProperties;
import com.zuehlke.carrera.javapilot.services.PilotService;
import com.zuehlke.carrera.javapilot.services.PilotToRelayConnection;
import com.zuehlke.carrera.javapilot.services.SimulatorService;
import com.zuehlke.carrera.simulator.config.SimulatorProperties;
import com.zuehlke.carrera.simulator.model.RaceTrackSimulatorSystem;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableConfigurationProperties({SimulatorProperties.class})  // loaded from classpath:/application.yml
public class PilotApplication implements CommandLineRunner{

    @Autowired
    private SimulatorService simulatorService;

    @Autowired
    private PilotService pilotService;

    @Autowired
    private PilotProperties pilotProperties;

    private enum Function {
        simulator,
        pilot,
        both
    }

    /**
     * Primary entry point of Carrera Simulator
     * @param args runtime arguments
     */
    public static void main(String[] args) {

        SpringApplication.run(PilotApplication.class, args);

    }

    @Override
    public void run(String... args) throws Exception {

        Options options = new Options();
        options.addOption("p", true, "Protocol: any of 'memory' (default), 'rabbit', or 'ws'");
        options.addOption("f", true, "either of 'simulator', 'pilot'. Defaults to 'both'. Requires rabbit");

        List<String> arglist = new ArrayList<>();
        for (String arg : args) {
            if (!arg.contains("--")) {
                arglist.add(arg);
            }
        }
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, arglist.toArray(new String[ arglist.size()]));

        Protocol protocol = Protocol.memory;
        if ( cmd.hasOption("p")) {
            protocol = Protocol.valueOf(cmd.getOptionValue("p"));
        }
        Function function = Function.both;
        if ( cmd.hasOption("f")) {
            function = Function.valueOf(cmd.getOptionValue("f"));
        }

        connectWithProtocol ( protocol, function );

    }

    private void connectWithProtocol(Protocol protocol, Function function ) {

        switch ( protocol ) {
            case rabbit: // only supporting rabbit for the time being
                // if not "only simulator", then connect the pilot
                if (!function.equals(Function.simulator)) {
                    connectPilotWithRabbit(pilotService.getPilotActor());
                }
                // if not "only pilot", then connect the simulator
                if (!function.equals(Function.pilot)) {
                    connectSimulatorWithRabbit(simulatorService.getSystem());
                }
        }
    }


    private void connectPilotWithRabbit(ActorRef pilot) {

        Client client = new RabbitClient();
        client.connect(pilotProperties.getRabbitUrl());
        Serializer serializer = new JacksonSerializer();
        PilotApi pilotApi = new DirectExchangePilotApiImpl(client, new PilotToRelayChannelNames(pilotProperties.getName()),
                new RoutingKeyNames(pilotProperties.getName()), serializer);

        ConnectionFactoryFromPilots factory = new RabbitConnectionFactoryFromPilots(pilotApi,
                pilotProperties.getName(), pilotProperties.getAccessCode(), pilotProperties.getRabbitUrl());

        PilotToRelayConnection pilotConnection = factory.create(
                (start)->pilot.tell(start, ActorRef.noSender()),
                (stop)->pilot.tell(stop, ActorRef.noSender()),
                (sensor)->pilot.tell(sensor, ActorRef.noSender()),
                (velo)->pilot.tell(velo, ActorRef.noSender()),
                (penalty)->pilot.tell(penalty, ActorRef.noSender()),
                (roundPassed)->pilot.tell(roundPassed, ActorRef.noSender())
        );

        pilotConnection.ensureConnection();
        pilot.tell(pilotConnection, ActorRef.noSender());
    }

    private void connectSimulatorWithRabbit (RaceTrackSimulatorSystem system ) {

        Client client = new RabbitClient();
        client.connect(pilotProperties.getRabbitUrl());
        Serializer serializer = new JacksonSerializer();
        TowardsPilotApi towardsPilotApi = new SimulatorTowardsPilotApiImpl(
                client, pilotProperties.getName(), new RoutingKeyNames(pilotProperties.getName()), serializer);

        ConnectionFactoryTowardsPilots factory = new RabbitConnectionFactoryTowardsPilots(towardsPilotApi);

        TowardsPilotsConnection towardsPilotsConnection = factory.create(system::setPower);

        system.register ( towardsPilotsConnection );

        towardsPilotsConnection.connect(pilotProperties.getRabbitUrl());

        simulatorService.setPilotConnection ( towardsPilotsConnection );

        client.subscribe("/app/pilots/announce", (m)->System.out.print("."));
    }
}
