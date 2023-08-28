package com.eppo.sdk.dto;

import com.eppo.sdk.deserializer.EppoValueDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * Eppo Custom value class
 */
@JsonDeserialize(using = EppoValueDeserializer.class)
public class EppoValue {
    private String value;
    private JsonNode node;
    private EppoValueType type = EppoValueType.NULL;
    private List<String> array;

    public EppoValue() {
    }

    public EppoValue(String value, EppoValueType type) {
        this.value = value;
        this.type = type;
    }

    public EppoValue(List<String> array) {
        this.array = array;
        this.type = EppoValueType.ARRAY_OF_STRING;
    }

    public EppoValue(JsonNode node) {
        this.node = node;
        this.value = node.toString();
        this.type = EppoValueType.JSON_NODE;
    }

    public EppoValue(EppoValueType type) {
        this.type = type;
    }

    public static EppoValue valueOf(String value) {
        return new EppoValue(value, EppoValueType.STRING);
    }

    public static EppoValue valueOf(double value) {
        return new EppoValue(Double.toString(value), EppoValueType.NUMBER);
    }

    public static EppoValue valueOf(boolean value) {
        return new EppoValue(Boolean.toString(value), EppoValueType.BOOLEAN);
    }

    public static EppoValue valueOf(JsonNode node) {
        return new EppoValue(node);
    }

    public static EppoValue valueOf(List<String> value) {
        return new EppoValue(value);
    }

    public static EppoValue valueOf() {
        return new EppoValue(EppoValueType.NULL);
    }

    public double doubleValue() {
        return Double.parseDouble(value);
    }

    public String stringValue() {
        return value;
    }

    public boolean boolValue() {
        return Boolean.valueOf(value);
    }

    public JsonNode jsonNodeValue() {
        return this.node;
    }

    public List<String> arrayValue() {
        return array;
    }

    public boolean isNumeric() {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isArray() {
        return type == EppoValueType.ARRAY_OF_STRING;
    }

    public boolean isBool() {
        return type == EppoValueType.BOOLEAN;
    }

    public boolean isNull() {
        return type == EppoValueType.NULL;
    }

    @Override
    public String toString() {
        if (this.isArray()) {
            return List.of(this.array).toString();
        } else if (this.type == EppoValueType.JSON_NODE) {
            return this.node.toString();
        }
        return this.value;
    }
}
