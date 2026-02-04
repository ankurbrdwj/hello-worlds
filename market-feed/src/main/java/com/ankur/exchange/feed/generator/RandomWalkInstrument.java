package com.ankur.exchange.feed.generator;

import com.ankur.exchange.feed.model.Instrument;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Getter
public class RandomWalkInstrument {
    private static final Random random = new Random();

    private final Instrument instrument;
    private long step;
    private int startPrice;
    private int targetPrice;
    private int targetSteps;
    private BigDecimal lastPrice;
    private double stepWidth;

    public RandomWalkInstrument(Instrument instrument) {
        this.instrument = instrument;
        this.step = 0;
        this.startPrice = random.nextInt(50, 500);
        this.targetPrice = generateTargetPrice();
        this.targetSteps = random.nextInt(10, 50);
        this.lastPrice = BigDecimal.valueOf(startPrice);
        this.stepWidth = calculateStepWidth();
    }

    private int generateTargetPrice() {
        int change = random.nextInt(-50, 51);
        return Math.max(startPrice + change, 1);
    }

    private double calculateStepWidth() {
        if (targetSteps > 0) {
            return (double)(targetPrice - startPrice) / targetSteps;
        }
        return 0.0;
    }

    private void reset() {
        this.startPrice = lastPrice.intValue();
        this.targetPrice = generateTargetPrice();
        this.targetSteps = random.nextInt(10, 50);
        this.step = 0;
        this.stepWidth = calculateStepWidth();
    }

    public BigDecimal nextPrice() {
        step++;
        if (step >= targetSteps) {
            reset();
        }

        double noise = randomBetween(-0.5, 0.5);
        lastPrice = lastPrice.add(BigDecimal.valueOf(stepWidth + noise));

        if (lastPrice.compareTo(BigDecimal.ONE) < 0) {
            lastPrice = BigDecimal.ONE;
        }

        return lastPrice.setScale(4, RoundingMode.HALF_UP);
    }

    private double randomBetween(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}