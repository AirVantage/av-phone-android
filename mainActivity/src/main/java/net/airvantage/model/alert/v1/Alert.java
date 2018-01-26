package net.airvantage.model.alert.v1;

public class Alert {

    public long date;
    public String uid;

    public static class Rule {
        public String name;
        public String message;
        public String uid;
    }
}
