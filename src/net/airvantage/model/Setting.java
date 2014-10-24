package net.airvantage.model;

public class Setting extends Data {

    public Setting(String id, String label, String type) {
        super(id, label, "setting");
        this.type = type;
    }

}
