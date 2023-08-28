package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.EppoValue;
import com.eppo.sdk.dto.SubjectAttributes;
import com.eppo.sdk.exception.InvalidSubjectAttribute;
import com.eppo.sdk.dto.Condition;
import com.eppo.sdk.dto.Rule;

import java.util.List;
import java.util.Optional;
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
    public static boolean compareNumber(double a, double b, IConditionFunc<Double> conditionFunc) {
        return conditionFunc.check(a, b);
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
        return values.stream()
                .map(value -> value.toLowerCase())
                .collect(Collectors.toList())
                .indexOf(a.toLowerCase()) >= 0;
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
     */
    public static Optional<Rule> findMatchingRule(
            SubjectAttributes subjectAttributes,
            List<Rule> rules
    ) {
        for (Rule rule : rules) {
            if (RuleValidator.matchesRule(subjectAttributes, rule)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
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
            SubjectAttributes subjectAttributes,
            Rule rule
    ) throws InvalidSubjectAttribute {
        List<Boolean> conditionEvaluations = RuleValidator.evaluateRuleConditions(subjectAttributes, rule.getConditions());
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
            SubjectAttributes subjectAttributes,
            Condition condition
    ) throws InvalidSubjectAttribute {
        if (subjectAttributes.containsKey(condition.attribute)) {
            EppoValue value = subjectAttributes.get(condition.attribute);
            try {
                switch (condition.operator) {
                    case GTE:
                        return Compare.compareNumber(value.doubleValue(), condition.value.doubleValue()
                                , (a, b) -> a >= b);
                    case GT:
                        return Compare.compareNumber(value.doubleValue(), condition.value.doubleValue(), (a, b) -> a > b);
                    case LTE:
                        return Compare.compareNumber(value.doubleValue(), condition.value.doubleValue(), (a, b) -> a <= b);
                    case LT:
                        return Compare.compareNumber(value.doubleValue(), condition.value.doubleValue(), (a, b) -> a < b);
                    case MATCHES:
                        return Compare.compareRegex(value.stringValue(), Pattern.compile(condition.value.stringValue()));
                    case ONE_OF:
                        return Compare.isOneOf(value.stringValue(), condition.value.arrayValue());
                    case NOT_ONE_OF:
                        return !Compare.isOneOf(value.stringValue(), condition.value.arrayValue());
                }
            } catch (Exception e) {
                return false;
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
            SubjectAttributes subjectAttributes,
            List<Condition> conditions
    ) throws InvalidSubjectAttribute {
        return conditions.stream()
                .map((condition) -> {
                    try {
                        return RuleValidator.evaluateCondition(subjectAttributes, condition);
                    } catch (Exception e) {
                        throw e;
                    }
                }).collect(Collectors.toList());
    }

}
