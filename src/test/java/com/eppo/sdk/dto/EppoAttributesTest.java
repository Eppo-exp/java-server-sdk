package com.eppo.sdk.dto;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashMap;
import java.util.Map;

class EppoAttributesTest {

    @Test
    void testSerializeEppoAttributesToJSONString() throws JSONException {
        EppoAttributes eppoAttributes = new EppoAttributes();
        eppoAttributes.put("boolean", EppoValue.valueOf(false));
        eppoAttributes.put("number", EppoValue.valueOf(1.234));
        eppoAttributes.put("string", EppoValue.valueOf("hello"));
        eppoAttributes.put("null", EppoValue.nullValue());

        String serializedJSONString = eppoAttributes.serializeToJSONString();
        String expectedJson = "{ \"boolean\": false, \"number\": 1.234, \"string\": \"hello\", \"null\": null }";

        JSONAssert.assertEquals(expectedJson, serializedJSONString, true);

        // Try omitting nulls now
        serializedJSONString = EppoAttributes.serializeNonNullAttributesToJSONString(eppoAttributes);
        expectedJson = "{ \"boolean\": false, \"number\": 1.234, \"string\": \"hello\" }";

        JSONAssert.assertEquals(expectedJson, serializedJSONString, true);
    }

    @Test
    void testSerializeNumericAttributesToJSONString()  throws JSONException {
        Map<String, Double> numericAttributes = new HashMap<>();
        numericAttributes.put("positive", 12.3);
        numericAttributes.put("negative", -45.6);
        numericAttributes.put("integer", 43.0);
        numericAttributes.put("null", null);

        String serializedJSONString = EppoAttributes.serializeAttributesToJSONString(numericAttributes);
        String expectedJson = "{ \"positive\": 12.3, \"negative\": -45.6, \"integer\": 43, \"null\": null }";

        JSONAssert.assertEquals(expectedJson, serializedJSONString, true);

        // Try omitting nulls now
        serializedJSONString = EppoAttributes.serializeNonNullAttributesToJSONString(numericAttributes);
        expectedJson = "{ \"positive\": 12.3, \"negative\": -45.6, \"integer\": 43 }";

        JSONAssert.assertEquals(expectedJson, serializedJSONString, true);
    }

    @Test
    void testSerializeCategoricalAttributesToJSONString()  throws JSONException {
        Map<String, String> categoricalAttributes = new HashMap<>();
        categoricalAttributes.put("a", "apple");
        categoricalAttributes.put("b", "banana");
        categoricalAttributes.put("null", null);

        String serializedJSONString = EppoAttributes.serializeAttributesToJSONString(categoricalAttributes);
        String expectedJson = "{ \"a\": \"apple\", \"b\": \"banana\", \"null\": null }";

        JSONAssert.assertEquals(expectedJson, serializedJSONString, true);

        // Try omitting nulls now
        serializedJSONString = EppoAttributes.serializeNonNullAttributesToJSONString(categoricalAttributes);
        expectedJson = "{ \"a\": \"apple\", \"b\": \"banana\" }";

        JSONAssert.assertEquals(expectedJson, serializedJSONString, true);
    }
}
