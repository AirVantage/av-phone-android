package net.airvantage.model;

import java.util.List;

public class Data {
    public String id;
    private String label;
    public String type;
    private String elementType;
    public List<Data> data;

    public Data(String id, String label, String elementType) {
        this.id = id;
        this.label = label;
        this.elementType = elementType;
    }
}
