package com.sierrawireless.avphone.service;

import java.util.Date;

public class CustomDataSource {

    // Rate of growth (in units / seconds)
    private static final float UP1_RATE = 0.2F;
    private static final float UP2_RATE = 0.05F;

    // Rate of decrease (in units / seconds) ; when 0 is reached, values
    // go back to 100.
    private static final float DOWN1_RATE = 0.2F;
    private static final float DOWN2_RATE = 0.05F;

    // How often (in seconds) should strings be changed ?
    private static final int STR1_DELAY = 3600;
    private static final int STR2_DELAY = 2 * 3600;

    public int customUp1 = 0;
    public int customUp2 = 0;
    public int customDown1 = 100;
    public int customDown2 = 100;

    public String customStr1 = "";
    public String customStr2 = "";

    public int customStr1Life = 0;
    public int customStr2Life = 0;

    private long lastTick;

    public CustomDataSource(Date date) {
        this.lastTick = date.getTime();
        updateStrs();
    }

    private void updateStrs() {
        updateStr1();
        updateStr2();
    }

    private void updateStr1() {
        customStr1 = "SN" + (lastTick / 10000);
    }

    private void updateStr2() {
        customStr2 = "SN" + ((lastTick / 10000) + ((int) Math.random() * 4200));
    }

    public Integer getCustomIntUp1() {
        return customUp1;
    }

    public Integer getCustomIntUp2() {
        return customUp2;
    }

    public Integer getCustomIntDown1() {
        return customDown1;
    }

    public Integer getCustomIntDown2() {
        return customDown2;
    }

    public String getCustomStr1() {
        return customStr1;
    }

    public String getCustomStr2() {
        return customStr2;
    }

    public void next(Date date) {
        long now = date.getTime();

        long delay = (long) ((now - lastTick) / 1000);

        customUp1 = customUp1 + (int) (delay * UP1_RATE);
        customUp2 = customUp2 + (int) (delay * UP2_RATE);

        customDown1 = customDown1 - (int) (delay * DOWN1_RATE);
        if (customDown1 < 0) {
            customDown1 = 100;
        }

        customDown2 = customDown2 - (int) (delay * DOWN2_RATE);
        if (customDown2 < 0) {
            customDown2 = 100;
        }

        customStr1Life += delay;
        customStr2Life += delay;

        lastTick = now;

        if (customStr1Life > STR1_DELAY) {
            updateStr1();
        }

        if (customStr2Life > STR2_DELAY) {
            updateStr2();
        }

    }

}
