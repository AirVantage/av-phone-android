package net.airvantage.model;

import java.util.List;

public class Command extends Data {

    public List<Parameter> parameters;

    public Command(String id, String label) {
        super(id, label, "command");
    }

}
