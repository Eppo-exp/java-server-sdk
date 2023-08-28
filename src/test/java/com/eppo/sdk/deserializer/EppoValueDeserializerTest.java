package com.eppo.sdk.deserializer;

import com.eppo.sdk.dto.EppoValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SingleValue<T> {
    public T value;
}

class SingleEppoValue extends SingleValue<EppoValue> {}

class EppoValueDeserializerTest {

    private ObjectMapper mapper = new ObjectMapper();

    @DisplayName("Test deserializing double")
    @Test
    void testDeserializingDouble() throws Exception {
        SingleEppoValue object = mapper.readValue("{\"value\": 1}", SingleEppoValue.class);
        Assertions.assertEquals(object.value.doubleValue(), 1);
    }

    @DisplayName("Test deserializing boolean")
    @Test
    void testDeserializingBoolean() throws Exception {
        SingleEppoValue object = mapper.readValue("{\"value\": true}", SingleEppoValue.class);
        Assertions.assertEquals(object.value.boolValue(), true);
    }

    @DisplayName("Test deserializing string")
    @Test
    void testDeserializingString() throws Exception {
        SingleEppoValue object = mapper.readValue("{\"value\": \"true\"}", SingleEppoValue.class);
        Assertions.assertEquals(object.value.stringValue(), "true");
    }

    @DisplayName("Test deserializing array")
    @Test
    void testDeserializingArray() throws Exception {
        SingleEppoValue object = mapper.readValue("{\"value\": [\"value1\", \"value2\"]}", SingleEppoValue.class);
        Assertions.assertTrue(object.value.arrayValue().contains("value1"));
    }

    @DisplayName("Test deserializing null")
    @Test
    void testDeserializingNull() throws Exception {
        SingleEppoValue object = mapper.readValue("{\"value\": null}", SingleEppoValue.class);
        Assertions.assertTrue(object.value == null);
    }

    @DisplayName("Test deserializing random object")
    @Test
    void testDeserializingRandomObject() throws Exception {
        SingleEppoValue object = mapper.readValue("{\"value\": {\"test\" : \"test\"}}", SingleEppoValue.class);
        Assertions.assertTrue(object.value.jsonNodeValue().get("test").textValue().compareTo("test") == 0);
    }
}