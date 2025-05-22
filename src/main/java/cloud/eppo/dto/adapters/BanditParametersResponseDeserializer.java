package cloud.eppo.dto.adapters;

import cloud.eppo.ufc.dto.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanditParametersResponseDeserializer
    extends StdDeserializer<BanditParametersResponse> {
  private static final Logger log =
      LoggerFactory.getLogger(BanditParametersResponseDeserializer.class);

  // Note: public default constructor is required by Jackson
  public BanditParametersResponseDeserializer() {
    this(null);
  }

  protected BanditParametersResponseDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public BanditParametersResponse deserialize(
      JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);
    if (rootNode == null || !rootNode.isObject()) {
      log.warn("no top-level JSON object");
      return new BanditParametersResponse();
    }

    JsonNode banditsNode = rootNode.get("bandits");
    if (banditsNode == null || !banditsNode.isObject()) {
      log.warn("no root-level bandits object");
      return new BanditParametersResponse();
    }

    Map<String, BanditParameters> bandits = new HashMap<>();
    banditsNode
        .iterator()
        .forEachRemaining(
            banditNode -> {
              String banditKey = banditNode.get("banditKey").asText();
              String updatedAtStr = banditNode.get("updatedAt").asText();
              Instant instant = Instant.parse(updatedAtStr);
              Date updatedAt = Date.from(instant);
              String modelName = banditNode.get("modelName").asText();
              String modelVersion = banditNode.get("modelVersion").asText();
              JsonNode modelDataNode = banditNode.get("modelData");
              double gamma = modelDataNode.get("gamma").asDouble();
              double defaultActionScore = modelDataNode.get("defaultActionScore").asDouble();
              double actionProbabilityFloor =
                  modelDataNode.get("actionProbabilityFloor").asDouble();
              JsonNode coefficientsNode = modelDataNode.get("coefficients");
              Map<String, BanditCoefficients> coefficients = new HashMap<>();
              Iterator<Map.Entry<String, JsonNode>> coefficientIterator = coefficientsNode.fields();
              coefficientIterator.forEachRemaining(
                  field -> {
                    BanditCoefficients actionCoefficients =
                        this.parseActionCoefficientsNode(field.getValue());
                    coefficients.put(field.getKey(), actionCoefficients);
                  });

              BanditModelData modelData =
                  new BanditModelData(
                      gamma, defaultActionScore, actionProbabilityFloor, coefficients);
              BanditParameters parameters =
                  new BanditParameters(banditKey, updatedAt, modelName, modelVersion, modelData);
              bandits.put(banditKey, parameters);
            });

    return new BanditParametersResponse(bandits);
  }

  private BanditCoefficients parseActionCoefficientsNode(JsonNode actionCoefficientsNode) {
    String actionKey = actionCoefficientsNode.get("actionKey").asText();
    Double intercept = actionCoefficientsNode.get("intercept").asDouble();

    JsonNode subjectNumericAttributeCoefficientsNode =
        actionCoefficientsNode.get("subjectNumericCoefficients");
    Map<String, BanditNumericAttributeCoefficients> subjectNumericAttributeCoefficients =
        this.parseNumericAttributeCoefficientsArrayNode(subjectNumericAttributeCoefficientsNode);
    JsonNode subjectCategoricalAttributeCoefficientsNode =
        actionCoefficientsNode.get("subjectCategoricalCoefficients");
    Map<String, BanditCategoricalAttributeCoefficients> subjectCategoricalAttributeCoefficients =
        this.parseCategoricalAttributeCoefficientsArrayNode(
            subjectCategoricalAttributeCoefficientsNode);

    JsonNode actionNumericAttributeCoefficientsNode =
        actionCoefficientsNode.get("actionNumericCoefficients");
    Map<String, BanditNumericAttributeCoefficients> actionNumericAttributeCoefficients =
        this.parseNumericAttributeCoefficientsArrayNode(actionNumericAttributeCoefficientsNode);
    JsonNode actionCategoricalAttributeCoefficientsNode =
        actionCoefficientsNode.get("actionCategoricalCoefficients");
    Map<String, BanditCategoricalAttributeCoefficients> actionCategoricalAttributeCoefficients =
        this.parseCategoricalAttributeCoefficientsArrayNode(
            actionCategoricalAttributeCoefficientsNode);

    return new BanditCoefficients(
        actionKey,
        intercept,
        subjectNumericAttributeCoefficients,
        subjectCategoricalAttributeCoefficients,
        actionNumericAttributeCoefficients,
        actionCategoricalAttributeCoefficients);
  }

  private Map<String, BanditNumericAttributeCoefficients>
      parseNumericAttributeCoefficientsArrayNode(JsonNode numericAttributeCoefficientsArrayNode) {
    Map<String, BanditNumericAttributeCoefficients> numericAttributeCoefficients = new HashMap<>();
    numericAttributeCoefficientsArrayNode
        .iterator()
        .forEachRemaining(
            numericAttributeCoefficientsNode -> {
              String attributeKey = numericAttributeCoefficientsNode.get("attributeKey").asText();
              Double coefficient = numericAttributeCoefficientsNode.get("coefficient").asDouble();
              Double missingValueCoefficient =
                  numericAttributeCoefficientsNode.get("missingValueCoefficient").asDouble();
              BanditNumericAttributeCoefficients coefficients =
                  new BanditNumericAttributeCoefficients(
                      attributeKey, coefficient, missingValueCoefficient);
              numericAttributeCoefficients.put(attributeKey, coefficients);
            });

    return numericAttributeCoefficients;
  }

  private Map<String, BanditCategoricalAttributeCoefficients>
      parseCategoricalAttributeCoefficientsArrayNode(
          JsonNode categoricalAttributeCoefficientsArrayNode) {
    Map<String, BanditCategoricalAttributeCoefficients> categoricalAttributeCoefficients =
        new HashMap<>();
    categoricalAttributeCoefficientsArrayNode
        .iterator()
        .forEachRemaining(
            categoricalAttributeCoefficientsNode -> {
              String attributeKey =
                  categoricalAttributeCoefficientsNode.get("attributeKey").asText();
              Double missingValueCoefficient =
                  categoricalAttributeCoefficientsNode.get("missingValueCoefficient").asDouble();

              Map<String, Double> valueCoefficients = new HashMap<>();
              JsonNode valuesNode = categoricalAttributeCoefficientsNode.get("valueCoefficients");
              Iterator<Map.Entry<String, JsonNode>> coefficientIterator = valuesNode.fields();
              coefficientIterator.forEachRemaining(
                  field -> {
                    String value = field.getKey();
                    Double coefficient = field.getValue().asDouble();
                    valueCoefficients.put(value, coefficient);
                  });

              BanditCategoricalAttributeCoefficients coefficients =
                  new BanditCategoricalAttributeCoefficients(
                      attributeKey, missingValueCoefficient, valueCoefficients);
              categoricalAttributeCoefficients.put(attributeKey, coefficients);
            });

    return categoricalAttributeCoefficients;
  }
}
