package com.eppo.sdk.deserializer;

import com.eppo.sdk.dto.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class BanditsDeserializer extends StdDeserializer<Map<String, BanditParameters>> {

    // Note: public default constructor is required by Jackson
    public BanditsDeserializer() {
        this(null);
    }

    protected BanditsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Map<String, BanditParameters> deserialize(
            JsonParser jsonParser,
            DeserializationContext deserializationContext
    ) throws IOException {
        JsonNode banditsNode = jsonParser.getCodec().readTree(jsonParser);
        Map<String, BanditParameters> bandits = new HashMap<>();
        banditsNode.iterator().forEachRemaining(banditNode -> {
            String banditKey = banditNode.get("banditKey").asText();
            String updatedAtStr = banditNode.get("updatedAt").asText();
            Instant instant = Instant.parse(updatedAtStr);
            Date updatedAt = Date.from(instant);
            String modelName = banditNode.get("modelName").asText();
            String modelVersion = banditNode.get("modelVersion").asText();

            BanditParameters parameters = new BanditParameters();
            parameters.setBanditKey(banditKey);
            parameters.setUpdatedAt(updatedAt);
            parameters.setModelName(modelName);
            parameters.setModelVersion(modelVersion);

            BanditModelData modelData = new BanditModelData();
            JsonNode modelDataNode = banditNode.get("modelData");
            double gamma = modelDataNode.get("gamma").asDouble();
            modelData.setGamma(gamma);
            double defaultActionScore = modelDataNode.get("defaultActionScore").asDouble();
            modelData.setDefaultActionScore(defaultActionScore);
            double actionProbabilityFloor = modelDataNode.get("actionProbabilityFloor").asDouble();
            modelData.setActionProbabilityFloor(actionProbabilityFloor);
            JsonNode coefficientsNode = modelDataNode.get("coefficients");

            Map<String, BanditCoefficients> coefficients = new HashMap<>();
            coefficientsNode.iterator().forEachRemaining(actionCoefficientsNode -> {
                BanditCoefficients actionCoefficients = this.parseActionCoefficientsNode(actionCoefficientsNode);
                coefficients.put(actionCoefficients.getActionKey(), actionCoefficients);
            });

            modelData.setCoefficients(coefficients);

            parameters.setModelData(modelData);
            bandits.put(banditKey, parameters);
        });
        return bandits;
    }

    private BanditCoefficients parseActionCoefficientsNode(JsonNode actionCoefficientsNode) {
        String actionKey = actionCoefficientsNode.get("actionKey").asText();
        Double intercept = actionCoefficientsNode.get("intercept").asDouble();

        JsonNode subjectNumericAttributeCoefficientsNode = actionCoefficientsNode.get("subjectNumericCoefficients");
        Map<String, BanditNumericAttributeCoefficients> subjectNumericAttributeCoefficients = this.parseNumericAttributeCoefficientsArrayNode(subjectNumericAttributeCoefficientsNode);
        JsonNode subjectCategoricalAttributeCoefficientsNode = actionCoefficientsNode.get("subjectCategoricalCoefficients");
        Map<String, BanditCategoricalAttributeCoefficients> subjectCategoricalAttributeCoefficients = this.parseCategoricalAttributeCoefficientsArrayNode(subjectCategoricalAttributeCoefficientsNode);

        JsonNode actionNumericAttributeCoefficientsNode = actionCoefficientsNode.get("actionNumericCoefficients");
        Map<String, BanditNumericAttributeCoefficients> actionNumericAttributeCoefficients = this.parseNumericAttributeCoefficientsArrayNode(actionNumericAttributeCoefficientsNode);
        JsonNode actionCategoricalAttributeCoefficientsNode = actionCoefficientsNode.get("actionCategoricalCoefficients");
        Map<String, BanditCategoricalAttributeCoefficients> actionCategoricalAttributeCoefficients = this.parseCategoricalAttributeCoefficientsArrayNode(actionCategoricalAttributeCoefficientsNode);

        BanditCoefficients coefficients = new BanditCoefficients();
        coefficients.setActionKey(actionKey);
        coefficients.setIntercept(intercept);
        coefficients.setSubjectNumericCoefficients(subjectNumericAttributeCoefficients);
        coefficients.setSubjectCategoricalCoefficients(subjectCategoricalAttributeCoefficients);
        coefficients.setActionNumericCoefficients(actionNumericAttributeCoefficients);
        coefficients.setActionCategoricalCoefficients(actionCategoricalAttributeCoefficients);
        return coefficients;
    }

    private Map<String, BanditNumericAttributeCoefficients> parseNumericAttributeCoefficientsArrayNode(JsonNode numericAttributeCoefficientsArrayNode) {
        Map<String, BanditNumericAttributeCoefficients> numericAttributeCoefficients = new HashMap<>();
        numericAttributeCoefficientsArrayNode.iterator().forEachRemaining(numericAttributeCoefficientsNode -> {
            String attributeKey = numericAttributeCoefficientsNode.get("attributeKey").asText();
            Double coefficient = numericAttributeCoefficientsNode.get("coefficient").asDouble();
            Double missingValueCoefficient = numericAttributeCoefficientsNode.get("missingValueCoefficient").asDouble();

            BanditNumericAttributeCoefficients coefficients = new BanditNumericAttributeCoefficients();
            coefficients.setAttributeKey(attributeKey);
            coefficients.setCoefficient(coefficient);
            coefficients.setMissingValueCoefficient(missingValueCoefficient);
            numericAttributeCoefficients.put(attributeKey, coefficients);
        });

        return numericAttributeCoefficients;
    }

    private Map<String, BanditCategoricalAttributeCoefficients> parseCategoricalAttributeCoefficientsArrayNode(JsonNode categoricalAttributeCoefficientsArrayNode) {
        Map<String, BanditCategoricalAttributeCoefficients> categoricalAttributeCoefficients = new HashMap<>();
        categoricalAttributeCoefficientsArrayNode.iterator().forEachRemaining(categoricalAttributeCoefficientsNode -> {
            String attributeKey = categoricalAttributeCoefficientsNode.get("attributeKey").asText();
            Double missingValueCoefficient = categoricalAttributeCoefficientsNode.get("missingValueCoefficient").asDouble();
            Map<String, Double> valueCoefficients = new HashMap<>();
            JsonNode valuesNode = categoricalAttributeCoefficientsNode.get("values");
            valuesNode.iterator().forEachRemaining(valueNode -> {
                String value = valueNode.get("value").asText();
                Double coefficient = valueNode.get("coefficient").asDouble();
                valueCoefficients.put(value, coefficient);
            });

            BanditCategoricalAttributeCoefficients coefficients = new BanditCategoricalAttributeCoefficients();
            coefficients.setAttributeKey(attributeKey);
            coefficients.setValueCoefficients(valueCoefficients);
            coefficients.setMissingValueCoefficient(missingValueCoefficient);
            categoricalAttributeCoefficients.put(attributeKey, coefficients);
        });

        return categoricalAttributeCoefficients;
    }
}
