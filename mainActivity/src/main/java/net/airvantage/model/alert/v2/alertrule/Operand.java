package net.airvantage.model.alert.v2.alertrule;

import java.util.List;

public class Operand {

    public AttributeId attributeId;
    public String function;
    public String functionParam;
    public String valueStr;
    public Number valueNum;
    public List<String> valuesStr;
}
