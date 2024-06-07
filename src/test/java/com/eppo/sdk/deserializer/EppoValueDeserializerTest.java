package com.eppo.sdk.deserializer;

import cloud.eppo.rac.dto.EppoValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EppoValueDeserializerTest {

  private final ObjectMapper mapper = new ObjectMapper();

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
    Assertions.assertTrue(object.value.boolValue());
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
    SingleEppoValue object =
        mapper.readValue("{\"value\": [\"value1\", \"value2\"]}", SingleEppoValue.class);
    Assertions.assertTrue(object.value.arrayValue().contains("value1"));
  }

  @DisplayName("Test deserializing null")
  @Test
  void testDeserializingNull() throws Exception {
    SingleEppoValue object = mapper.readValue("{\"value\": null}", SingleEppoValue.class);
    Assertions.assertNull(object.value);
  }

  @DisplayName("Test deserializing random object")
  @Test
  void testDeserializingRandomObject() throws Exception {
    SingleEppoValue object =
        mapper.readValue("{\"value\": {\"test\" : \"test\"}}", SingleEppoValue.class);
    Assertions.assertEquals(
        0, object.value.jsonNodeValue().get("test").textValue().compareTo("test"));
  }

  static class SingleValue<T> {
    public T value;
  }

  static class SingleEppoValue extends SingleValue<EppoValue> {
  }
}
