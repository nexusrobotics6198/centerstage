package net.nexusrobotics.swiftbot.odometry;

import net.nexusrobotics.swiftbot.constants.MecanumConstants;
import net.nexusrobotics.swiftbot.localization.Localization;
import net.nexusrobotics.swiftbot.robot.MecanumRobot;

public class Odometry {
    MecanumRobot robot;
    MecanumConstants constants;
    Localization localization;
    int timederivative = 10;
    float odoDiameter;

    public Odometry(MecanumRobot robot, Localization localization){
        this.constants = robot.constants;
        this.robot = robot;
        this.localization = localization;
        this.odoDiameter = constants.deadwheeldiameter;
    }
    int threshold = 10;
    public void init(){
        new Thread(()->{
            int lastPosP1 = robot.getOdoParallel1().getCurrentPosition();
            int lastPosP2 = robot.getOdoParallel2().getCurrentPosition();
            int lastPosPerp = robot.getOdoPerp().getCurrentPosition();
            while(true){
                int dP1 = robot.getOdoParallel1().getCurrentPosition() - lastPosP1;
                int dP2 = robot.getOdoParallel2().getCurrentPosition() - lastPosP2;
                int dPerp = robot.getOdoPerp().getCurrentPosition() - lastPosPerp;

                double odoCircumference = odoDiameter*Math.PI;

                double dOdoPerp = odoCircumference*(dPerp/constants.ticks_per_rotation); // in same unit as diameter (inches)
                double dOdoP1 = odoCircumference*(dP1/constants.ticks_per_rotation); // in same unit as diameter (inches)
                double dOdoP2 = odoCircumference*(dP2/constants.ticks_per_rotation); // in same unit as diameter (inches)

                if((dP1 > 0)&&(dP2 < 0) || (dP2 > 0)&&(dP1 < 0)){ // check if parallel odo wheels are moving in opposite directions
                    // 1 is right, 2 is left
                    int direction = dPerp > 0 ? 2 : 1; // may be debug idk
                    localization.setHeading((float) (localization.currentHeading + (360*(dOdoPerp/(2*constants.odoperptocenter*Math.PI)))));
                }else/* if((dP1 - dP2 > threshold && ((dP1 > 0 && dP2 > 0) || (dP1 < 0 && dP2 < 0)))||(dP1 + dP2 > threshold && ((dP1 < 0 && dP2 > 0) || (dP1 > 0 && dP2 < 0))))*/ {
                    // update heading and lateral positioning
                    float yMultiplier;
                    float xMultiplier;
                    localization.setHeading((float) (localization.currentHeading + (360*(dOdoPerp/(2*constants.odoperptocenter*Math.PI)))));
                    if((localization.currentHeading > 0 && localization.currentHeading < 90) || (localization.currentHeading > 270 && localization.currentHeading < 360)){
                        // positive y increase
                       yMultiplier = 1;
                    }else{
                        yMultiplier = -1;
                    }
                    if((localization.currentHeading > 0 && localization.currentHeading < 180)){
                        // positive x increase
                        xMultiplier = 1;
                    }else{
                        xMultiplier = -1;
                    }
                    float heading = localization.currentHeading;
                    float nearestCardinal = (heading / 90) * 90;
                    float distance = Math.min(Math.abs(heading - nearestCardinal), Math.abs(360 - (heading - nearestCardinal)));
                    float y =  1.0f - ((float) distance / 90.0f);
                    yMultiplier = y*yMultiplier;
                    xMultiplier = (1-y)*xMultiplier;
                    localization.setX((float) (localization.currentX + ((dOdoP1+dOdoP2)/2))*xMultiplier);
                    localization.setY((float) (localization.currentY + ((dOdoP1+dOdoP2)/2))*yMultiplier);
                }/*else if((dP1>0&&dP2>0)||(dP1<0&&dP2<0)){
                    localization.setX((float) (localization.currentX + ((dOdoP1+dOdoP2)/2)));

                }*/

                    lastPosP1 = robot.getOdoParallel1().getCurrentPosition();
                    lastPosP2 = robot.getOdoParallel2().getCurrentPosition();
                    lastPosPerp = robot.getOdoPerp().getCurrentPosition();
                try {
                    Thread.sleep(timederivative);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}