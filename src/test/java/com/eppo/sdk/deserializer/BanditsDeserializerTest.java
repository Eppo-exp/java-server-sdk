package com.eppo.sdk.deserializer;

import com.eppo.sdk.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BanditsDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testDeserializingBandits() throws IOException {
        String jsonString = FileUtils.readFileToString(new File("src/test/resources/bandits/bandits-parameters-1.json"), "UTF8");
        BanditParametersResponse responseObject = this.mapper.readValue(jsonString, BanditParametersResponse.class);

        assertEquals(2, responseObject.getBandits().size());
        BanditParameters parameters = responseObject.getBandits().get("banner-bandit");
        assertEquals("banner-bandit", parameters.getBanditKey());
        assertEquals("falcon", parameters.getModelName());
        assertEquals("v123", parameters.getModelVersion());

        BanditModelData modelData = parameters.getModelData();
        assertEquals(1.0, modelData.getGamma());
        assertEquals(0.0, modelData.getDefaultActionScore());
        assertEquals(0.0, modelData.getActionProbabilityFloor());

        Map<String, BanditCoefficients> coefficients = modelData.getCoefficients();
        assertEquals(2, coefficients.size());

        // Nike

        BanditCoefficients nikeCoefficients = coefficients.get("nike");
        assertEquals("nike", nikeCoefficients.getActionKey());
        assertEquals(1.0, nikeCoefficients.getIntercept());

        // Nike subject coefficients

        Map<String, BanditNumericAttributeCoefficients> nikeSubjectNumericCoefficients = nikeCoefficients.getSubjectNumericCoefficients();
        assertEquals(1, nikeSubjectNumericCoefficients.size());

        BanditNumericAttributeCoefficients nikeAccountAgeCoefficients = nikeSubjectNumericCoefficients.get("account_age");
        assertEquals("account_age", nikeAccountAgeCoefficients.getAttributeKey());
        assertEquals(0.3, nikeAccountAgeCoefficients.getCoefficient());
        assertEquals(0.0, nikeAccountAgeCoefficients.getMissingValueCoefficient());

        Map<String, BanditCategoricalAttributeCoefficients> nikeSubjectCategoricalCoefficients = nikeCoefficients.getSubjectCategoricalCoefficients();
        assertEquals(1, nikeSubjectCategoricalCoefficients.size());

        BanditCategoricalAttributeCoefficients nikeGenderIdentityCoefficients = nikeSubjectCategoricalCoefficients.get("gender_identity");
        assertEquals("gender_identity", nikeGenderIdentityCoefficients.getAttributeKey());
        assertEquals(2.3, nikeGenderIdentityCoefficients.getMissingValueCoefficient());
        Map<String, Double> nikeGenderIdentityCoefficientValues = nikeGenderIdentityCoefficients.getValueCoefficients();
        assertEquals(2, nikeGenderIdentityCoefficientValues.size());
        assertEquals(0.5, nikeGenderIdentityCoefficientValues.get("female"));
        assertEquals(-0.5, nikeGenderIdentityCoefficientValues.get("male"));

        // Nike action coefficients

        Map<String, BanditNumericAttributeCoefficients> nikeActionNumericCoefficients = nikeCoefficients.getActionNumericCoefficients();
        assertEquals(1, nikeActionNumericCoefficients.size());

        BanditNumericAttributeCoefficients nikeBrandAffinityCoefficient = nikeActionNumericCoefficients.get("brand_affinity");
        assertEquals("brand_affinity", nikeBrandAffinityCoefficient.getAttributeKey());
        assertEquals(1.0, nikeBrandAffinityCoefficient.getCoefficient());
        assertEquals(-0.1, nikeBrandAffinityCoefficient.getMissingValueCoefficient());

        Map<String, BanditCategoricalAttributeCoefficients> nikeActionCategoricalCoefficients = nikeCoefficients.getActionCategoricalCoefficients();
        assertEquals(1, nikeActionCategoricalCoefficients.size());

        BanditCategoricalAttributeCoefficients nikeLoyaltyCoefficients = nikeActionCategoricalCoefficients.get("loyalty_tier");
        assertEquals("loyalty_tier", nikeLoyaltyCoefficients.getAttributeKey());
        assertEquals(0.0, nikeLoyaltyCoefficients.getMissingValueCoefficient());
        Map<String, Double> nikeLoyaltyCoefficientValues = nikeLoyaltyCoefficients.getValueCoefficients();
        assertEquals(3, nikeLoyaltyCoefficientValues.size());
        assertEquals(4.5, nikeLoyaltyCoefficientValues.get("gold"));
        assertEquals(3.2, nikeLoyaltyCoefficientValues.get("silver"));
        assertEquals(1.9, nikeLoyaltyCoefficientValues.get("bronze"));

        // Adidas

        BanditCoefficients adidasCoefficients = coefficients.get("adidas");
        assertEquals("adidas", adidasCoefficients.getActionKey());
        assertEquals(1.1, adidasCoefficients.getIntercept());

        // Adidas subject coefficients

        Map<String, BanditNumericAttributeCoefficients> adidasSubjectNumericCoefficients = adidasCoefficients.getSubjectNumericCoefficients();
        assertEquals(0, adidasSubjectNumericCoefficients.size());

        Map<String, BanditCategoricalAttributeCoefficients> adidasSubjectCategoricalCoefficients = adidasCoefficients.getSubjectCategoricalCoefficients();
        assertEquals(1, adidasSubjectCategoricalCoefficients.size());

        BanditCategoricalAttributeCoefficients adidasGenderIdentityCoefficient = adidasSubjectCategoricalCoefficients.get("gender_identity");
        assertEquals("gender_identity", adidasGenderIdentityCoefficient.getAttributeKey());
        assertEquals(0.45, adidasGenderIdentityCoefficient.getMissingValueCoefficient());
        Map<String, Double> adidasGenderIdentityCoefficientValues = adidasGenderIdentityCoefficient.getValueCoefficients();
        assertEquals(2, adidasGenderIdentityCoefficientValues.size());
        assertEquals(0.0, adidasGenderIdentityCoefficientValues.get("female"));
        assertEquals(0.3, adidasGenderIdentityCoefficientValues.get("male"));

        // Adidas action coefficients

        Map<String, BanditNumericAttributeCoefficients> adidasActionNumericCoefficients = adidasCoefficients.getActionNumericCoefficients();
        assertEquals(1, nikeActionNumericCoefficients.size());

        BanditNumericAttributeCoefficients adidasBrandAffinityCoefficient = adidasActionNumericCoefficients.get("brand_affinity");
        assertEquals("brand_affinity", adidasBrandAffinityCoefficient.getAttributeKey());
        assertEquals(2.0, adidasBrandAffinityCoefficient.getCoefficient());
        assertEquals(1.2, adidasBrandAffinityCoefficient.getMissingValueCoefficient());

        Map<String, BanditCategoricalAttributeCoefficients> adidasActionCategoricalCoefficients = adidasCoefficients.getActionCategoricalCoefficients();
        assertEquals(1, adidasActionCategoricalCoefficients.size());

        BanditCategoricalAttributeCoefficients adidasPurchasedLast30Coefficient = adidasActionCategoricalCoefficients.get("purchased_last_30_days");
        assertEquals("purchased_last_30_days", adidasPurchasedLast30Coefficient.getAttributeKey());
        assertEquals(0.0, adidasPurchasedLast30Coefficient.getMissingValueCoefficient());
        Map<String, Double> adidasPurchasedLast30CoefficientValues = adidasPurchasedLast30Coefficient.getValueCoefficients();
        assertEquals(2, adidasPurchasedLast30CoefficientValues.size());
        assertEquals(9.0, adidasPurchasedLast30CoefficientValues.get("true"));
        assertEquals(0.0, adidasPurchasedLast30CoefficientValues.get("false"));
    }
}
