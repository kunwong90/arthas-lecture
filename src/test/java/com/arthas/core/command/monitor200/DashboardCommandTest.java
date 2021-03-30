package com.arthas.core.command.monitor200;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class DashboardCommandTest {


    @Test
    public void processTest() {
        DashboardCommand dashboardCommand = new DashboardCommand();
        dashboardCommand.process();

        try {
            TimeUnit.DAYS.sleep(1);
        } catch (Exception e) {

        }
    }

}