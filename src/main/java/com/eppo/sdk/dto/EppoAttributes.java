package com.eppo.sdk.dto;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Subject Attributes Class
 */
public class EppoAttributes extends HashMap<String, EppoValue> {

  public EppoAttributes() {
    super();
  }

  public EppoAttributes(Map<String, EppoValue> initialValues) {
    super(initialValues);
  }

  public String serializeToJSONString() {
    return EppoAttributes.serializeAttributesToJSONString(this);
  }

  public static String serializeAttributesToJSONString(Map<String, ?> attributes) {
    return EppoAttributes.serializeAttributesToJSONString(attributes, false);
  }

  public static String serializeNonNullAttributesToJSONString(Map<String, ?> attributes) {
    return EppoAttributes.serializeAttributesToJSONString(attributes, true);
  }

  private static String serializeAttributesToJSONString(Map<String, ?> attributes, boolean omitNulls) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode result = mapper.createObjectNode();

    for (Map.Entry<String, ?> entry : attributes.entrySet()) {
      String attributeName = entry.getKey();
      Object attributeValue = entry.getValue();

      if (attributeValue instanceof EppoValue) {
        EppoValue eppoValue = (EppoValue)attributeValue;
        if (eppoValue.isNull()) {
          if (!omitNulls) {
            result.putNull(attributeName);
          }
          continue;
        }
        if (eppoValue.isNumeric()) {
          result.put(attributeName, eppoValue.doubleValue());
          continue;
        }
        if (eppoValue.isBoolean()) {
          result.put(attributeName, eppoValue.boolValue());
          continue;
        }
        // fall back put treating any other eppo values as a string
        result.put(attributeName, eppoValue.toString());
      } else if (attributeValue instanceof Double) {
        Double doubleValue = (Double)attributeValue;
        result.put(attributeName, doubleValue);
      } else if (attributeValue == null) {
        if (!omitNulls) {
          result.putNull(attributeName);
        }
      } else {
        // treat everything else as a string
        result.put(attributeName, attributeValue.toString());
      }
    }

    try {
      return mapper.writeValueAsString(result);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
