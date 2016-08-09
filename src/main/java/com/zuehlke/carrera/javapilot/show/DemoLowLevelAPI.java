package com.zuehlke.carrera.javapilot.show;

import com.rabbitmq.client.*;
import com.zuehlke.carrera.api.channel.PilotToRelayChannelNames;
import com.zuehlke.carrera.api.channel.RoutingKeyNames;
import com.zuehlke.carrera.api.client.PublishException;
import com.zuehlke.carrera.api.seralize.JacksonSerializer;
import com.zuehlke.carrera.relayapi.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the usage of the low level Rabbit API for the FnF challenge
 *
 *
 *
 */
public class DemoLowLevelAPI {

    // This is assuming that rabbitmq-server is running on this localhost
    private static final String rabbitUri = "amqp://localhost";

    // This name has to be provided with all messages, so that the relay knows who you are.
    private static final String PILOT_NAME = "testpilot";

    // This should be the access code you've been provided by race management
    private static final String ACCESS_CODE = "SECRET";

    private static final int NUMBER_OF_MESSAGES_EXPECTED = 8;

    private Connection connection;
    private PilotToRelayChannelNames channelNames = new PilotToRelayChannelNames(PILOT_NAME);
    private RoutingKeyNames routingKeyNames = new RoutingKeyNames(PILOT_NAME);

    private static final Logger logger = LoggerFactory.getLogger("DEMOS");

    private AtomicInteger numberOfMessagesReceived = new AtomicInteger(0);


    private class ChannelAndQueueName {
        Channel channel;
        String queueName;
        ChannelAndQueueName(Channel channel, String queueName ) {
            this.channel = channel;
            this.queueName = queueName;
        }
    }
    private class ChannelAndKeyAndQueueName extends ChannelAndQueueName {
        String keyName;
        ChannelAndKeyAndQueueName(Channel channel, String keyName, String queueName ) {
            super(channel, queueName);
            this.keyName = keyName;
        }
    }

    private Map<String, ChannelAndQueueName> channels = new HashMap<>();

    public static void main ( String [] args ) {

        new DemoLowLevelAPI().demo();
    }

    private void demo() {

        connectToRabbit_or_exit( rabbitUri );

        createChannelsInBothDirections();

        registerConsumers();

        publishMessages();

        while ( ! all_messages_received() ) {
            waitASecond();
        }

        shutdown();
    }


    private void connectToRabbit_or_exit ( String uri ) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setUri(uri);
            connection = connectionFactory.newConnection();
            connection.addShutdownListener(cause -> {
                logger.info("Connection was shut down. Reason: {}", cause.getReason().protocolMethodName());
            });
        } catch (Exception e) {
            logger.error ( "Couldn't connect to URL: {}", rabbitUri);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * creates channels from the pilot to the relay and the other way round
     * https://www.rabbitmq.com/tutorials/tutorial-four-java.html
     */
    private void createChannelsInBothDirections() {

        // From the pilot to the relay (and then to racetrack)
        // This is a fixed named queue "app/pilots/announce"
        createQueue( "pilot lifesigns", channelNames.announce());

        // Also rom the pilot to the relay (and then to racetrack)
        // This is a queue linked to an exchange by a routing key
        createRouting( "power controls", routingKeyNames.powerControl());

        // from the racetrack (via relay) to the pilot
        // These are all queues linked to an exchange by their resp. routing key
        createRouting( "sensor values", routingKeyNames.sensor());
        createRouting( "velocity", routingKeyNames.velocity());
        createRouting( "penalty", routingKeyNames.penalty());
        createRouting( "start", routingKeyNames.raceStart());
        createRouting( "stop", routingKeyNames.raceStop());
        createRouting( "round passed", routingKeyNames.roundPassed());
    }

    /**
     * publish one message to each queue
     */
    private void publishMessages() {

        // from the pilot
        publish(channelNames.announce(), announcement(PILOT_NAME));
        publish(routingKeyNames.powerControl(), powerControl(PILOT_NAME));

        // from the racetrack/relay
        publish(routingKeyNames.raceStart(), raceStart(PILOT_NAME));
        publish(routingKeyNames.raceStop(), raceStop(PILOT_NAME));
        publish(routingKeyNames.sensor(), sensor());
        publish(routingKeyNames.roundPassed(), roundPassed(PILOT_NAME));
        publish(routingKeyNames.velocity(), velocity());
        publish(routingKeyNames.penalty(), penalty());
    }

    private ChannelAndKeyAndQueueName createChannelFromExchange (String routingKeyName ) {

        try {
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(PILOT_NAME, "direct");
            String queueName = channel.queueDeclare().getQueue();
            return new ChannelAndKeyAndQueueName(channel, routingKeyName, queueName );
        } catch ( Exception e ) {
            logger.error ( "Couldn't create channel for routing: {}", routingKeyName);
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    private void createQueue(String messageDescription, String queueName ) {
        logger.info("creating queue for {}: {}", messageDescription, queueName);
        Channel channel = createChannel(queueName);
        channels.put(queueName, new ChannelAndQueueName(channel, queueName) );
    }

    private void createRouting(String messageDescription, String routingKeyName ) {
        logger.info("creating routing for {}: {}", messageDescription, routingKeyName);
        ChannelAndKeyAndQueueName caq = createChannelFromExchange(routingKeyName);
        channels.put(caq.keyName, caq);
    }

    /**
     * register the consumers that will receive messages from the pilot.
     * This could be the relay or the simulator.
     */
    private void registerConsumers() {

        // Life signs
        registerConsumerOnQueue("relay", channelNames.announce());

        // Power Control
        registerConsumerOnExchange("relay", routingKeyNames.powerControl());

        // Race Start Messages
        registerConsumerOnExchange("pilot", routingKeyNames.raceStart());

        // Race Stop Messages
        registerConsumerOnExchange("pilot", routingKeyNames.raceStop());

        // Sensor values
        registerConsumerOnExchange("pilot", routingKeyNames.sensor());

        // round times
        registerConsumerOnExchange("pilot", routingKeyNames.roundPassed());

        // velocity
        registerConsumerOnExchange("pilot", routingKeyNames.velocity());

        // penalty
        registerConsumerOnExchange("pilot", routingKeyNames.penalty());

    }

    private void registerConsumerOnQueue(String function, String queueName ) {
        try {
            logger.info("registering as {} on {}", function, queueName);
            Channel channel = channels.get(queueName).channel;
            channel.basicConsume(queueName, createConsumer(channel, function));
        } catch ( Exception e ) {
            logger.error("Couldn't register opposite listeners");
            System.exit(-1);
        }
    }

    private void registerConsumerOnExchange(String function, String routingKey ) {
        try {
            logger.info("registering as {} on exchange with {}", function, routingKey);
            Channel channel = channels.get(routingKey).channel;
            String queueName = channels.get(routingKey).queueName;
            channel.queueBind(queueName, PILOT_NAME, routingKey);
            channel.basicConsume(queueName, createConsumer(channel, function));
        } catch ( Exception e ) {
            logger.error("Couldn't register opposite listeners");
            System.exit(-1);
        }
    }



    public void publish(String keyOrQueueName, String message) {
        try {
            Channel channel = channels.get(keyOrQueueName).channel;
            String queueName = channels.get(keyOrQueueName).queueName;
            AMQP.BasicProperties publishProperties = createPublishProperties();

            if ( queueName.equals(keyOrQueueName) ) {
                logger.info("Publishing {} to queue: {}", message, keyOrQueueName);
                channel.basicPublish("", queueName, publishProperties, message.getBytes());
            } else {
                logger.info("Publishing {} to exchange {} and with key {}", message, PILOT_NAME, keyOrQueueName);
                channel.basicPublish(PILOT_NAME, keyOrQueueName, publishProperties, message.getBytes());
            }
        } catch (IOException e) {
            throw new PublishException("Could not publish message", e);
        }
    }



    private AMQP.BasicProperties createPublishProperties() {
        return new AMQP.BasicProperties.Builder().expiration("0").build();
    }

    private void waitASecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("Interrupted during sleep.");
        }
    }


    /**
     * returns a simple consumer that
     * @param channel the channel to listen to
     * @return a consumer to act upon messages coming through that channel
     */
    private DefaultConsumer createConsumer(Channel channel, String function) {

        return new DefaultConsumer(channel ) {

            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) {

                logger.info ( "As {}: received message {}", function, new String(body));
                numberOfMessagesReceived.incrementAndGet();
            }
        };
    }


    private boolean all_messages_received() {
        return numberOfMessagesReceived.get() == NUMBER_OF_MESSAGES_EXPECTED;
    }

    /**
     * creates a queue with the given name.
     * Here, every channel has exactly one queue, and for every queue we have a distinct channel.
     * And every queue is made for only one message type.
     * @param channelName the name of the queue*
     * @return the new channel
     */
    private Channel createChannel(String channelName ) {
        try {
            Channel channel = connection.createChannel();
            boolean durable = true;
            boolean exclusive = false;
            boolean autoDelete = false;
            channel.queueDeclare(channelName, durable, exclusive, autoDelete, null);
            return channel;
        } catch ( Exception e ) {
            logger.error ( "Couldn't create channel: {}", channelName);
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }



    private String announcement(String pilotName) {
        // The URL is not used at the moment, thus truly optional.
        PilotLifeSign message = new PilotLifeSign(pilotName, ACCESS_CODE, "some.url.com", System.currentTimeMillis());
        return new JacksonSerializer().serialize(message);
    }

    private String powerControl(String pilotName) {
        PowerControl powerControl = new PowerControl(100, pilotName, ACCESS_CODE, System.currentTimeMillis());
        return new JacksonSerializer().serialize( powerControl);
    }

    private String raceStart (String pilotName ) {
        // None of the race start message's fields actually matter to the pilot. The fields are used for internal purposes.
        RaceStartMessage message = new RaceStartMessage("TheTrack", "test", pilotName, System.currentTimeMillis(), "test", false);
        return new JacksonSerializer().serialize(message);
    }

    private String raceStop ( String pilotName ) {
        RaceStopMessage message = new RaceStopMessage("TheTrack", pilotName, System.currentTimeMillis(), "test");
        return new JacksonSerializer().serialize(message);
    }

    private String penalty () {
        PenaltyMessage message = new PenaltyMessage("TheTrack", "0000", 200.0, 200.0, 2000 );
        return new JacksonSerializer().serialize(message);
    }

    private String velocity () {
        VelocityMessage message = new VelocityMessage("TheTrack", System.currentTimeMillis(), 200.0, "0000");
        return new JacksonSerializer().serialize(message);
    }

    private String sensor () {
        int [] acc = new int[] { 23, 234, 3454 };
        int [] mag = new int[] { 123, -7834, 354 };
        int [] gyr = new int[] { -25, 234, -3454 };
        SensorEvent message = new SensorEvent("Track", acc, gyr, mag, System.currentTimeMillis());
        return new JacksonSerializer().serialize(message);
    }

    private String roundPassed ( String pilotName ) {
        RoundTimeMessage message = new RoundTimeMessage("TheTrack", pilotName, System.currentTimeMillis(), 5678 );
        return new JacksonSerializer().serialize(message);
    }

    private void shutdown() {
        try {
            for (ChannelAndQueueName caq : channels.values()) {
                caq.channel.close();
            }
            connection.close();
        } catch ( Exception e ) {
            logger.error("Failed to close channels and the connection");
        }
    }


}
