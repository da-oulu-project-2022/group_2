package com.example.mobile;

/*
 * Class for acceleration data analysis:
 * Identification of punches, if the hand dropped after connecting,
 * calculation of punch velocity.
 * Currently prints out results to console, this has to be changed for use in the app.
 *
 * Author: Nicolas Schmitt
 * Date: 1.12.2022
 */

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PunchAnalyzer {
    private int SAMPLING_RATE;
    private int FRAME_LENGTH_IN_MS;
    public static final double MPS_TO_KMH = 3.6;   // m/s -> km/h

    // Punch result
    public boolean isPunch;
    public boolean isCorrectPunch;
    public float mySpeed;

    // Storage of 20 consecutive raw x values, meanSquareRoot in these buffers
    private List<Float> xValueBuffer = new ArrayList<>();
    private List<Double> meanSquareRootBuffer = new ArrayList<>();

    // for punch identification, a specified number of frames has to be ignored
    // (exact number depends on the sampling rate, see constructor)
    private int PUNCH_BLOCKED_FRAMES;
    private int MISTAKE_BLOCKED_FRAMES;

    // used for counting ignored frames / frames for mistake identification
    private int ignoreCount = 0;
    private boolean ignoreFrames = false;
    private int identificationCount = 0;

    // x value thresholds for registering punches and arm drops
    private double X_PUNCH_THRESHOLD = 75.0;
    private double X_MISTAKE_THRESHOLD = -20.0;

    private double MILLI_G_TO_METER_PER_SQUARE_SECOND = 9.81 / 1000.0;

    public PunchAnalyzer(int samplingRate) {

        for(int i = 0; i < 20; ++i) {       // buffer size should always stay the same after
            xValueBuffer.add((float) i + 1);   // adding data!
            meanSquareRootBuffer.add((double) i + 1);
        }

        SAMPLING_RATE = samplingRate;
        FRAME_LENGTH_IN_MS = 1000 / SAMPLING_RATE;
        PUNCH_BLOCKED_FRAMES = samplingRate / 5;
        MISTAKE_BLOCKED_FRAMES = 2 * PUNCH_BLOCKED_FRAMES;
    }

    public void nextFrame(float x, float y, float z) {
        // Log.d("My draft", "--------------------------------- nextFrame worked");

        x *= MILLI_G_TO_METER_PER_SQUARE_SECOND;
        y *= MILLI_G_TO_METER_PER_SQUARE_SECOND;
        z *= MILLI_G_TO_METER_PER_SQUARE_SECOND;

        double meanSquareRoot = Math.sqrt((((x * x) + (y * y) + (z * z)) / 3.0));
        xValueBuffer.add(x);                       // new element at the end,
        xValueBuffer.remove(0);              // oldest element removed
        meanSquareRootBuffer.add(meanSquareRoot);
        meanSquareRootBuffer.remove(0);

        analyzeX(x);
    }

    /*
     * Analyzes current data buffer for punches and mistakes, calls calculatePunchVelocity()
     * when a punch is recognized
     */
    private void analyzeX(float X) {
        /*
         * Punch recognition:
         * if x value is at least 75, a punch is registered and calculatePunchVelocity() is called
         * next 5 data frames are ignored to prevent registering
         * multiple punches
         */
        if(X >= X_PUNCH_THRESHOLD && !ignoreFrames) {
            Log.d("Algorithm", "X over 75: Punch recognized");  // Punch recognised

            isPunch = true;
            ignoreFrames = true;
            ignoreCount = PUNCH_BLOCKED_FRAMES;

            calculatePunchVelocity();
        }

        /*
         * Identifying if arm is dropped after punch:
         * after the skipped frames for punch recognition,
         * check if x drops below -20 at least once in the next 10
         * frames
         */
        if (ignoreFrames) {
            --ignoreCount;
            if (ignoreCount <= 0) {
                ignoreFrames = false;
                identificationCount = MISTAKE_BLOCKED_FRAMES;       // starts correct/incorrect identification
            }
        }

        if (identificationCount > 0) {
            --identificationCount;
            if (X < X_MISTAKE_THRESHOLD) {                  // correct punch recognised
                Log.d("Algorithm", "correct Punch!");
                isCorrectPunch = true;
                identificationCount = 0;        // reset to prevent counting multiple times
            } else if (identificationCount == 0) {
                Log.d("Algorithm", "Incorrect Punch!");
                isCorrectPunch = false;
            }
        }

        // Log.d("My draft", "--------------------------------- analyzeX");
    }

    /*
     * Calculates Punch velocity, currently prints result to console
     */
    private void calculatePunchVelocity() {
        // Log.d("My draft", "--------------------------------- calculatePunchVelocity");

        int startIndex= findStartIndex();
        int endIndex = findEndIndex();

        Log.d("Algorithm", "Start Index: " + startIndex);
        Log.d("Algorithm", "End Index: " + endIndex);

        if(startIndex == -1 || endIndex == -1) {
            Log.d("Algorithm", "Speed calculation: Index error!");
            return;
        }

        // ------ speed calculation     ------

        float speedInMps = 0;
        float speedMSR = 0;
        for (int i = startIndex; i < endIndex - 1; ++i) {
            speedInMps += (FRAME_LENGTH_IN_MS * Math.abs(xValueBuffer.get(i)));
            speedMSR += (FRAME_LENGTH_IN_MS * Math.abs(meanSquareRootBuffer.get(i)));
            if (i < endIndex - 2) {
                speedInMps += ((FRAME_LENGTH_IN_MS * Math.abs(xValueBuffer.get(i)
                        - xValueBuffer.get(i + 1))) / 2.0);
                speedMSR += ((FRAME_LENGTH_IN_MS * Math.abs(meanSquareRootBuffer.get(i)
                        - meanSquareRootBuffer.get(i + 1))) / 2.0);
            }
            else {
                speedInMps += (((FRAME_LENGTH_IN_MS / 2.0) * Math.abs(xValueBuffer.get(i))) / 2.0);
                speedMSR += (((FRAME_LENGTH_IN_MS / 2.0) * Math.abs(meanSquareRootBuffer.get(i))) / 2.0);
            }
            speedInMps -= FRAME_LENGTH_IN_MS * 10.0;
            speedMSR -= FRAME_LENGTH_IN_MS * 5.74;
        }

        speedInMps /= 1000; //framelength is in ms
        speedMSR /= 1000;

        Log.d("Algorithm", "Apprx. speed in meters per second: " + speedInMps);
        Log.d("Algorithm", "Apprx. speed in kilometer per hour: " + (speedInMps * MPS_TO_KMH));
        Log.d("Algorithm", "Apprx. speed(MSR) in meters per second: " + speedMSR);
        Log.d("Algorithm", "Apprx. speed(MSR) in kilometer per hour: " + (speedMSR * MPS_TO_KMH));

        mySpeed = speedMSR;
        // ------ end speed calculation ------

    }

    /*
     * Finds the Element indicating that the punching movement begins
     */
    private int findStartIndex() {

        int startIndex = 0;

        int frameBufferIndex = xValueBuffer.size() - 5;      // last elements are usually > 0!
        boolean continueStartSearch = true;


        while (continueStartSearch) {                       // search for element > -10.0 starts here,

            if (xValueBuffer.get(frameBufferIndex) > -10.0) {
                startIndex = frameBufferIndex;
                continueStartSearch = false;
                for (int i = 0; i < xValueBuffer.size(); ++i) {    // only for debugging
                    Log.d("Algorithm", "i: " + xValueBuffer.get(i));
                }
            }

            if (frameBufferIndex == 0) {
                continueStartSearch = false;
                Log.d("Algorithm", "Start not found!");
                startIndex = -1;
            }
            --frameBufferIndex;
        }
        return startIndex;
    }

    /*
     * Finds the Element indicating that the punch connects
     */
    private int findEndIndex() {
        int endIndex = 0;
        int frameBufferIndex = xValueBuffer.size() - 1;      // start from last element
        boolean continueEndSearch = true;

        while (continueEndSearch) {                       // search for element > -10.0 starts here,

            if (xValueBuffer.get(frameBufferIndex) < 0) {
                endIndex = frameBufferIndex;
                continueEndSearch = false;
            }

            if (frameBufferIndex == 0) {
                continueEndSearch = false;
                Log.d("Algorithm", "End not found!");
                endIndex = -1;
            }
            --frameBufferIndex;
        }

        return endIndex;
    }

}