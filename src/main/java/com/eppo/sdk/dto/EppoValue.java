package com.eppo.sdk.dto;

import com.eppo.sdk.deserializer.EppoValueDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Eppo Custom value class
 */
@JsonDeserialize(using = EppoValueDeserializer.class)
public class EppoValue {
    private final EppoValueType type;
    private String stringValue;
    private Double doubleValue;
    private Boolean boolValue;
    private JsonNode jsonValue;
    private List<String> stringArrayValue;

    private EppoValue(String stringValue) {
        this.stringValue = stringValue;
        this.type = stringValue != null ? EppoValueType.STRING : EppoValueType.NULL;
    }

    private EppoValue(Double doubleValue) {
        this.doubleValue = doubleValue;
        this.type = doubleValue != null ? EppoValueType.NUMBER : EppoValueType.NULL;
    }
    private EppoValue(Boolean boolValue) {
        this.boolValue = boolValue;
        this.type = boolValue != null ? EppoValueType.BOOLEAN : EppoValueType.NULL;
    }

    private EppoValue(List<String> stringArrayValue) {
        this.stringArrayValue = stringArrayValue;
        this.type = stringArrayValue != null ? EppoValueType.ARRAY_OF_STRING : EppoValueType.NULL;
    }

    private EppoValue(JsonNode jsonValue) {
        this.jsonValue = jsonValue;
        this.type = EppoValueType.JSON_NODE;
    }

    public static EppoValue valueOf(String stringValue) {
        return new EppoValue(stringValue);
    }

    public static EppoValue valueOf(double doubleValue) {
        return new EppoValue(doubleValue);
    }

    public static EppoValue valueOf(boolean boolValue) {
        return new EppoValue(boolValue);
    }

    public static EppoValue valueOf(JsonNode jsonValue) {
        return new EppoValue(jsonValue);
    }

    public static EppoValue valueOf(List<String> value) {
        return new EppoValue(value);
    }

    public static EppoValue nullValue() {
        return new EppoValue((String)null);
    }

    public double doubleValue() {
        return this.doubleValue;
    }

    public String stringValue() {
        return this.stringValue;
    }

    public boolean boolValue() {
        return this.boolValue;
    }

    public JsonNode jsonNodeValue() {
        return this.jsonValue;
    }

    public List<String> arrayValue() {
        return this.stringArrayValue;
    }

    public boolean isString() {
        return this.type == EppoValueType.STRING;
    }

    public boolean isNumeric() {
        return this.type == EppoValueType.NUMBER;
    }

    public boolean isBoolean() {
        return this.type == EppoValueType.BOOLEAN;
    }

    public boolean isArray() {
        return type == EppoValueType.ARRAY_OF_STRING;
    }

    public boolean isJson() {
        return type == EppoValueType.JSON_NODE;
    }

    public boolean isNull() {
        return type == EppoValueType.NULL;
    }

    @Override
    public String toString() {
        switch(this.type) {
            case STRING:
                return this.stringValue;
            case NUMBER:
                // By default, `String.valueOf(<double>)` will include at least one decimal place.
                // Though numeric flags can either be integers or floating-point types. And target
                // rule logic will cast a number type to a String before evaluating `oneOf` or `notOneOf`
                // rules.
                // The logic below ensures the cast to string better represents the intended numeric
                // field type.
                //
                // @see https://docs.geteppo.com/feature-flagging/flag-variations#numeric-flags
                // @see https://docs.geteppo.com/feature-flagging/targeting#supported-rule-operators
                if (this.doubleValue.intValue() == this.doubleValue) {
                    return String.valueOf(this.doubleValue.intValue());
                }
                return String.valueOf(this.doubleValue);
            case BOOLEAN:
                return this.boolValue.toString();
            case ARRAY_OF_STRING:
                return Collections.singletonList(this.stringArrayValue).toString();
            case JSON_NODE:
                return this.jsonValue.toString();
            default: // NULL
                return "";
        }
    }
}
