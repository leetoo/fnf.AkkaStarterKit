package com.zuehlke.carrera.javapilot.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.zuehlke.carrera.relayapi.messages.PenaltyMessage;
import com.zuehlke.carrera.relayapi.messages.RaceStartMessage;
import com.zuehlke.carrera.relayapi.messages.SensorEvent;
import com.zuehlke.carrera.relayapi.messages.VelocityMessage;
import com.zuehlke.carrera.timeseries.FloatingHistory;
import org.apache.commons.lang.StringUtils;

/**
 *  this logic node increases the power level by 10 units per 0.5 second until it receives a penalty
 *  then reduces by ten units.
 */
public class PowerUpUntilPenalty extends UntypedActor {

    private static final int SAFE_POWER = 100;
    private static final int MAX_POWER = 180; // Max for this phase;
    private static final int DURATION_BETWEEN_INCREASES=5000;

    private final ActorRef kobayashi;

    private double currentPower = 0;
    private long lastIncrease = 0;

    private boolean probing = true;

    private FloatingHistory gyrozHistory = new FloatingHistory(8);

    /**
     * @param pilotActor The central pilot actor
     * @return the actor props
     */
    public static Props props( ActorRef pilotActor) {
        return Props.create(
                PowerUpUntilPenalty.class, () -> new PowerUpUntilPenalty(pilotActor ));
    }

    public PowerUpUntilPenalty(ActorRef pilotActor) {
        lastIncrease = System.currentTimeMillis();
        this.kobayashi = pilotActor;
    }


    @Override
    public void onReceive(Object message) throws Exception {

        if ( message instanceof SensorEvent ) {
            handleSensorEvent((SensorEvent) message);

        } else if ( message instanceof VelocityMessage ) {
            handleVelocityMessage((VelocityMessage) message);

        } else if ( message instanceof PenaltyMessage) {
            handlePenaltyMessage ((PenaltyMessage) message);

        } else if ( message instanceof RaceStartMessage) {
            handleRaceStart();

        } else {
            unhandled(message);
        }
    }

    private void handleVelocityMessage(VelocityMessage message ) {
        System.out.printf("Received velocity message: %.2f m/s\n", message.getVelocity());
    }

    private void handleRaceStart() {
        currentPower = SAFE_POWER;
        lastIncrease = 0;
        probing = true;
        gyrozHistory = new FloatingHistory(8);
    }

    private void handlePenaltyMessage(PenaltyMessage message) {
        System.out.printf("Received penalty message: %.2f m/s, allowed %.2f\n", message.getActualSpeed(), message.getSpeedLimit());
        currentPower = Math.max( 0, currentPower - 10 );
        System.out.printf("Reducing power to %.2f\n", currentPower);
        kobayashi.tell(new PowerAction((int)currentPower), getSelf());
        probing = false;
    }

    /**
     * Strategy: increase quickly when standing still to overcome haptic friction
     * then increase slowly. Probing phase will be ended by the first penalty
     * @param message the sensor event coming in
     */
    private void handleSensorEvent(SensorEvent message) {

        show (message.getG()[2]);

        if (probing) {
            if (message.getTimeStamp() > lastIncrease + DURATION_BETWEEN_INCREASES) {
                increase(2);
                System.out.printf("After %d ms, increasing power to %.02f\n", lastIncrease, currentPower);
                lastIncrease = message.getTimeStamp();
            }
        }

        kobayashi.tell(new PowerAction((int)currentPower), getSelf());
    }

    private int increase ( double val ) {
        currentPower = Math.min ( currentPower + val, MAX_POWER);
        return (int)currentPower;
    }

    private void show(int gyr2) {
        int scale = 120 * (gyr2 - (-10000) ) / 20000;
        System.out.println(StringUtils.repeat(" ", scale) + gyr2);
    }

    @Override
    public void postStop () {
        System.out.println("Strategy actor stopped.");
    }

}
