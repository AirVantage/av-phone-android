package net.airvantage.model;

import java.util.List;

public class Data {
    public String id;
    public String label;
    public String type;
    public String description;
    public String elementType;
    public List<Data> data;

    public Data(String id, String label, String elementType) {
        this.id = id;
        this.label = label;
        this.elementType = elementType;
    }
}
