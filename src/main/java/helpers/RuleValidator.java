package helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.Condition;
import dto.OperatorType;
import dto.Rule;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

interface IConditionFunc<T> {
    public boolean check(T a, T b);
}

class Compare {
    public static boolean compareNumber(String a, String b, IConditionFunc<Long> conditionFunc) {
        return conditionFunc.check(Long.parseLong(a, 10), Long.parseLong(b, 10));
    }

    public static boolean compareRegex(String a, Pattern pattern) {
        return pattern.matcher(a).matches();
    }

    public static boolean isOneOf(String a, List<String> values) {
        return values.indexOf(a) >= 0;
    }
}

public class RuleValidator {
    public static boolean matchesAnyRule(Map<String, String> subjectAttributes, List<Rule> rules) throws Exception {
        for (Rule rule : rules) {
            if (RuleValidator.matchesRule(subjectAttributes, rule)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesRule(Map<String, String> subjectAttributes, Rule rule ) throws Exception {
        List<Boolean> conditionEvaluations = RuleValidator.evaluateRuleConditions(subjectAttributes, rule.conditions);
        return !conditionEvaluations.contains(false);
    }

    private static boolean evaluateCondition(Map<String, String> subjectAttributes, Condition condition) throws Exception {
        if (subjectAttributes.containsKey(condition.attribute)) {
            String value = subjectAttributes.get(condition.attribute);
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
                    List<String> values1 = mapper1.readValue(condition.value, new TypeReference<List<String>>(){});
                    return Compare.isOneOf(value, values1);
                case NOT_ONE_OF:
                    ObjectMapper mapper2 = new ObjectMapper();
                    List<String> values2 = mapper2.readValue(condition.value, new TypeReference<List<String>>(){});
                    return !Compare.isOneOf(value, values2);
            }
        }
        return false;
    }

    private static List<Boolean> evaluateRuleConditions(Map<String, String> subjectAttributes, List<Condition> conditions) throws Exception {
        return conditions.stream()
                .map((condition) -> {
                    try {
                        return RuleValidator.evaluateCondition(subjectAttributes, condition);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

}
