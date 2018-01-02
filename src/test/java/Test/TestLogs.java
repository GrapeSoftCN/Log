package Test;

import common.java.httpServer.booter;
import common.java.nlogger.nlogger;

public class TestLogs {
    public static void main(String[] args) {
        booter booter = new booter();
        try {
            System.out.println("GrapeLog");
            System.setProperty("AppName", "GrapeLog");
            booter.start(1006);
        } catch (Exception e) {
            nlogger.logout(e);
        }
    }
}
