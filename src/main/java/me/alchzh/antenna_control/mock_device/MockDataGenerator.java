package me.alchzh.antenna_control.mock_device;

import java.util.Random;

public class MockDataGenerator {
    private final Random fRandom = new Random();
    private float iMean;
    private float iStdDev;
    private float aStdDev;

    private float aMean;

    public MockDataGenerator(float iMean, float iStdDev, float aStdDev) {
        this.iMean = iMean;
        this.iStdDev = iStdDev;
        this.aStdDev = aStdDev;

        getNewMean();
    }

    public void setInitialMean(float iMean) {
        this.iMean = iMean;
    }

    public void setInitialStdDev(float aStdDev) {
        this.iStdDev = aStdDev;
    }

    public void getNewMean() {
        aMean = getGaussian(iMean, iStdDev);
    }

    public void setCollectedStdDev(float aStdDev) {
        this.aStdDev = aStdDev;
    }

    public float collectData() {
        return getGaussian(aMean, aStdDev);
    }

    private float getGaussian(float mean, float stdDev){
        return mean + (float)fRandom.nextGaussian() * stdDev;
    }
}
