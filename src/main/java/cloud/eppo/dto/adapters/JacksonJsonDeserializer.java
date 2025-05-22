package cloud.eppo.dto.adapters;

import cloud.eppo.Utils;
import cloud.eppo.api.EppoValue;
import cloud.eppo.exception.JsonParsingException;
import cloud.eppo.ufc.dto.BanditParametersResponse;
import cloud.eppo.ufc.dto.FlagConfigResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JacksonJsonDeserializer implements Utils.JsonDeserializer {

  private static final Logger log = LoggerFactory.getLogger(JacksonJsonDeserializer.class);
  public static final ObjectMapper mapper =
      new ObjectMapper().registerModule(EppoModule.eppoModule());

  @Override
  public FlagConfigResponse parseFlagConfigResponse(byte[] jsonString) throws JsonParsingException {
    try {
      return mapper.readValue(jsonString, FlagConfigResponse.class);
    } catch (IOException e) {
      throw new JsonParsingException(e);
    }
  }

  @Override
  public BanditParametersResponse parseBanditParametersResponse(byte[] jsonString)
      throws JsonParsingException {
    try {
      return mapper.readValue(jsonString, BanditParametersResponse.class);
    } catch (IOException e) {
      throw new JsonParsingException(e);
    }
  }

  @Override
  public boolean isValidJson(String json) {
    try {
      return mapper.readTree(json) != null;
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  @Override
  public String serializeAttributesToJSONString(Map<String, EppoValue> map, boolean omitNulls) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode result = mapper.createObjectNode();

    for (Map.Entry<String, EppoValue> entry : map.entrySet()) {
      String attributeName = entry.getKey();
      EppoValue attributeValue = entry.getValue();

      if (attributeValue == null || attributeValue.isNull()) {
        if (!omitNulls) {
          result.putNull(attributeName);
        }
      } else {
        if (attributeValue.isNumeric()) {
          result.put(attributeName, attributeValue.doubleValue());
          continue;
        }
        if (attributeValue.isBoolean()) {
          result.put(attributeName, attributeValue.booleanValue());
          continue;
        }
        // fall back put treating any other eppo values as a string
        result.put(attributeName, attributeValue.toString());
      }
    }
    try {
      return mapper.writeValueAsString(result);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static Date parseUtcISODateNode(JsonNode isoDateStringElement) {
    if (isoDateStringElement == null || isoDateStringElement.isNull()) {
      return null;
    }
    String isoDateString = isoDateStringElement.asText();
    return Utils.parseUtcISODateString(isoDateString);
  }
}
