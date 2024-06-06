package com.eppo.sdk.helpers;

import cloud.eppo.rac.dto.Condition;
import cloud.eppo.rac.dto.EppoAttributes;
import cloud.eppo.rac.dto.EppoValue;
import cloud.eppo.rac.dto.Rule;
import cloud.eppo.rac.exception.InvalidSubjectAttribute;
import com.github.zafarkhaja.semver.Version;
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

/** Compare Class */
class Compare {
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
        .map(String::toLowerCase)
        .collect(Collectors.toList())
        .contains(a.toLowerCase());
  }
}

/** Rule Validator Class */
public class RuleValidator {
  /**
   * This function is used to check if any rule is matched
   *
   * @param subjectAttributes
   * @param rules
   * @return
   */
  public static Optional<Rule> findMatchingRule(
      EppoAttributes subjectAttributes, List<Rule> rules) {
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
  private static boolean matchesRule(EppoAttributes subjectAttributes, Rule rule)
      throws InvalidSubjectAttribute {
    List<Boolean> conditionEvaluations =
        RuleValidator.evaluateRuleConditions(subjectAttributes, rule.getConditions());
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
  private static boolean evaluateCondition(EppoAttributes subjectAttributes, Condition condition)
      throws InvalidSubjectAttribute {
    if (subjectAttributes.containsKey(condition.getAttribute())) {
      EppoValue value = subjectAttributes.get(condition.getAttribute());
      Optional<Version> valueSemVer = Version.tryParse(value.stringValue());
      Optional<Version> conditionSemVer = Version.tryParse(condition.getValue().stringValue());

      try {
        switch (condition.getOperator()) {
          case GTE:
            if (value.isNumeric() && condition.getValue().isNumeric()) {
              return value.doubleValue() >= condition.getValue().doubleValue();
            }

            if (valueSemVer.isPresent() && conditionSemVer.isPresent()) {
              return valueSemVer.get().isHigherThanOrEquivalentTo(conditionSemVer.get());
            }

            return false;
          case GT:
            if (value.isNumeric() && condition.getValue().isNumeric()) {
              return value.doubleValue() > condition.getValue().doubleValue();
            }

            if (valueSemVer.isPresent() && conditionSemVer.isPresent()) {
              return valueSemVer.get().isHigherThan(conditionSemVer.get());
            }

            return false;
          case LTE:
            if (value.isNumeric() && condition.getValue().isNumeric()) {
              return value.doubleValue() <= condition.getValue().doubleValue();
            }

            if (valueSemVer.isPresent() && conditionSemVer.isPresent()) {
              return valueSemVer.get().isLowerThanOrEquivalentTo(conditionSemVer.get());
            }

            return false;
          case LT:
            if (value.isNumeric() && condition.getValue().isNumeric()) {
              return value.doubleValue() < condition.getValue().doubleValue();
            }

            if (valueSemVer.isPresent() && conditionSemVer.isPresent()) {
              return valueSemVer.get().isLowerThan(conditionSemVer.get());
            }

            return false;
          case MATCHES:
            return Compare.compareRegex(
                value.stringValue(), Pattern.compile(condition.getValue().stringValue()));
          case ONE_OF:
            return Compare.isOneOf(value.toString(), condition.getValue().arrayValue());
          case NOT_ONE_OF:
            return !Compare.isOneOf(value.toString(), condition.getValue().arrayValue());
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
      EppoAttributes subjectAttributes, List<Condition> conditions) throws InvalidSubjectAttribute {
    return conditions.stream()
        .map(
            (condition) -> {
              try {
                return RuleValidator.evaluateCondition(subjectAttributes, condition);
              } catch (Exception e) {
                throw e;
              }
            })
        .collect(Collectors.toList());
  }
}
