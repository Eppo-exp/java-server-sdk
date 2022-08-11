package com.eppo.sdk.helpers;

import com.eppo.sdk.exception.InvalidSubjectAttribute;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eppo.sdk.dto.Condition;
import com.eppo.sdk.dto.Rule;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Compare Function Interface
 *
 * @param <T>
 */
interface IConditionFunc<T> {
    public boolean check(T a, T b);
}

/**
 * Compare Class
 */
class Compare {
    /**
     * This function is used to compare number
     *
     * @param a
     * @param b
     * @param conditionFunc
     * @return
     */
    public static boolean compareNumber(String a, String b, IConditionFunc<Long> conditionFunc) {
        return conditionFunc.check(Long.parseLong(a, 10), Long.parseLong(b, 10));
    }

    /**
     * This function is used to compare Regex
     *
     * @param a
     * @param pattern
     * @return
     */
    public static boolean compareRegex(String a, Pattern pattern) {
        return pattern.matcher(a).matches();
    }

    /**
     * This function is used to compare one of
     *
     * @param a
     * @param values
     * @return
     */
    public static boolean isOneOf(String a, List<String> values) {
        return values.indexOf(a) >= 0;
    }
}

/**
 * Rule Validator Class
 */
public class RuleValidator {
    /**
     * This function is used to check if any rule is matched
     *
     * @param subjectAttributes
     * @param rules
     * @return
     * @throws InvalidSubjectAttribute
     */
    public static boolean matchesAnyRule(
            Map<String, String> subjectAttributes,
            List<Rule> rules
    ) throws InvalidSubjectAttribute {
        for (Rule rule : rules) {
            if (RuleValidator.matchesRule(subjectAttributes, rule)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This function is used to check if rule is matched
     *
     * @param subjectAttributes
     * @param rule
     * @return
     * @throws InvalidSubjectAttribute
     */
    private static boolean matchesRule(
            Map<String, String> subjectAttributes,
            Rule rule
    ) throws InvalidSubjectAttribute {
        List<Boolean> conditionEvaluations = RuleValidator.evaluateRuleConditions(subjectAttributes, rule.conditions);
        return !conditionEvaluations.contains(false);
    }

    /**
     * This function is used to check condition
     *
     * @param subjectAttributes
     * @param condition
     * @return
     * @throws InvalidSubjectAttribute
     */
    private static boolean evaluateCondition(
            Map<String, String> subjectAttributes,
            Condition condition
    ) throws InvalidSubjectAttribute {
        if (subjectAttributes.containsKey(condition.attribute)) {
            String value = subjectAttributes.get(condition.attribute);
            try {
                switch (condition.operator) {
                    case GTE:
                        return Compare.compareNumber(value, condition.value
                                , (a, b) -> a >= b);
                    case GT:
                        return Compare.compareNumber(value, condition.value, (a, b) -> a > b);
                    case LTE:
                        return Compare.compareNumber(value, condition.value, (a, b) -> a <= b);
                    case LT:
                        return Compare.compareNumber(value, condition.value, (a, b) -> a < b);
                    case MATCHES:
                        return Compare.compareRegex(value, Pattern.compile(condition.value));
                    case ONE_OF:
                        ObjectMapper mapper1 = new ObjectMapper();
                        List<String> values1 = mapper1.readValue(condition.value, new TypeReference<List<String>>() {
                        });
                        return Compare.isOneOf(value, values1);
                    case NOT_ONE_OF:
                        ObjectMapper mapper2 = new ObjectMapper();
                        List<String> values2 = mapper2.readValue(condition.value, new TypeReference<List<String>>() {
                        });
                        return !Compare.isOneOf(value, values2);
                }
            } catch (Exception e) {
                throw new InvalidSubjectAttribute("Invalid subject attribute : " + value);
            }

        }
        return false;
    }

    /**
     * This function is used to check conditions
     *
     * @param subjectAttributes
     * @param conditions
     * @return
     * @throws InvalidSubjectAttribute
     */
    private static List<Boolean> evaluateRuleConditions(
            Map<String, String> subjectAttributes,
            List<Condition> conditions
    ) throws InvalidSubjectAttribute {
        return conditions.stream()
                .map((condition) -> {
                    try {
                        return RuleValidator.evaluateCondition(subjectAttributes, condition);
                    } catch (Exception e) {
                        throw  e;
                    }
                }).collect(Collectors.toList());
    }

}
