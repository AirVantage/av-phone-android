package net.airvantage.model;

public class Variable extends Data {

    public Variable(String id, String label, String type) {
        super(id, label, "variable");
        this.type = type;
    }

}
