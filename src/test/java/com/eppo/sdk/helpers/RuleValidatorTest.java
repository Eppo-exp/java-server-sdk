package com.eppo.sdk.helpers;

import cloud.eppo.rac.dto.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleValidatorTest {
  public Rule createRule(List<Condition> conditions) {
    return new Rule(null, conditions);
  }

  public void addConditionToRule(Rule rule, Condition condition) {
    rule.getConditions().add(condition);
  }

  public void addNumericConditionToRule(Rule rule) {
    Condition condition1 = new Condition(OperatorType.GTE, "price", EppoValue.valueOf(10));
    Condition condition2 = new Condition(OperatorType.LTE, "price", EppoValue.valueOf(20));
    addConditionToRule(rule, condition1);
    addConditionToRule(rule, condition2);
  }

  public void addSemVerConditionToRule(Rule rule) {
    Condition condition1 = new Condition(OperatorType.GTE, "version", EppoValue.valueOf("1.5.0"));
    Condition condition2 = new Condition(OperatorType.LT, "version", EppoValue.valueOf("2.2.0"));
    addConditionToRule(rule, condition1);
    addConditionToRule(rule, condition2);
  }

  public void addRegexConditionToRule(Rule rule) {
    Condition condition = new Condition(OperatorType.MATCHES, "match", EppoValue.valueOf("[a-z]+"));
    addConditionToRule(rule, condition);
  }

  public void addOneOfCondition(Rule rule) {
    Condition condition =
        new Condition(
            OperatorType.ONE_OF, "oneOf", EppoValue.valueOf(Arrays.asList("value1", "value2")));
    addConditionToRule(rule, condition);
  }

  public void addOneOfConditionWithIntegers(Rule rule) {
    Condition condition =
        new Condition(OperatorType.ONE_OF, "oneOf", EppoValue.valueOf(Arrays.asList("1", "2")));
    addConditionToRule(rule, condition);
  }

  public void addOneOfConditionWithDoubles(Rule rule) {
    Condition condition =
        new Condition(OperatorType.ONE_OF, "oneOf", EppoValue.valueOf(Arrays.asList("1.5", "2.7")));
    addConditionToRule(rule, condition);
  }

  public void addOneOfConditionWithBoolean(Rule rule) {
    Condition condition =
        new Condition(
            OperatorType.ONE_OF, "oneOf", EppoValue.valueOf(Collections.singletonList("true")));
    addConditionToRule(rule, condition);
  }

  public void addNotOneOfCondition(Rule rule) {
    Condition condition =
        new Condition(
            OperatorType.NOT_ONE_OF, "oneOf", EppoValue.valueOf(Arrays.asList("value1", "value2")));
    addConditionToRule(rule, condition);
  }

  public void addNameToSubjectAttribute(EppoAttributes subjectAttributes) {
    subjectAttributes.put("name", EppoValue.valueOf("test"));
  }

  public void addPriceToSubjectAttribute(EppoAttributes subjectAttributes) {
    subjectAttributes.put("price", EppoValue.valueOf("30"));
  }

  @DisplayName("findMatchingRule() with empty conditions")
  @Test
  void testMatchesAnyRuleWithEmptyConditions() {
    List<Rule> rules = new ArrayList<>();
    final Rule ruleWithEmptyConditions = createRule(new ArrayList<>());
    rules.add(ruleWithEmptyConditions);
    EppoAttributes subjectAttributes = new EppoAttributes();
    addNameToSubjectAttribute(subjectAttributes);

    Assertions.assertEquals(
        ruleWithEmptyConditions, RuleValidator.findMatchingRule(subjectAttributes, rules).get());
  }

  @DisplayName("findMatchingRule() with empty rules")
  @Test
  void testMatchesAnyRuleWithEmptyRules() {
    List<Rule> rules = new ArrayList<>();
    EppoAttributes subjectAttributes = new EppoAttributes();
    addNameToSubjectAttribute(subjectAttributes);

    Assertions.assertFalse(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }

  @DisplayName("findMatchingRule() when no rule matches")
  @Test
  void testMatchesAnyRuleWhenNoRuleMatches() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addNumericConditionToRule(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    addPriceToSubjectAttribute(subjectAttributes);

    Assertions.assertFalse(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }

  @DisplayName("findMatchingRule() when rule matches")
  @Test
  void testMatchesAnyRuleWhenRuleMatches() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addNumericConditionToRule(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("price", EppoValue.valueOf(15));

    Assertions.assertEquals(rule, RuleValidator.findMatchingRule(subjectAttributes, rules).get());
  }

  @DisplayName("findMatchingRule() when rule matches with semver")
  @Test
  void testMatchesAnyRuleWhenRuleMatchesWithSemVer() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addSemVerConditionToRule(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("version", EppoValue.valueOf("1.15.5"));

    Assertions.assertEquals(rule, RuleValidator.findMatchingRule(subjectAttributes, rules).get());
  }

  @DisplayName("findMatchingRule() throw InvalidSubjectAttribute")
  @Test
  void testMatchesAnyRuleWhenThrowInvalidSubjectAttribute() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addNumericConditionToRule(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("price", EppoValue.valueOf("abcd"));

    Assertions.assertFalse(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }

  @DisplayName("findMatchingRule() with regex condition")
  @Test
  void testMatchesAnyRuleWithRegexCondition() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addRegexConditionToRule(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("match", EppoValue.valueOf("abcd"));

    Assertions.assertEquals(rule, RuleValidator.findMatchingRule(subjectAttributes, rules).get());
  }

  @DisplayName("findMatchingRule() with regex condition not matched")
  @Test
  void testMatchesAnyRuleWithRegexConditionNotMatched() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addRegexConditionToRule(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("match", EppoValue.valueOf("123"));

    Assertions.assertFalse(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }

  @DisplayName("findMatchingRule() with not oneOf rule")
  @Test
  void testMatchesAnyRuleWithNotOneOfRule() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addNotOneOfCondition(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf("value3"));

    Assertions.assertEquals(rule, RuleValidator.findMatchingRule(subjectAttributes, rules).get());
  }

  @DisplayName("findMatchingRule() with not oneOf rule not passed")
  @Test
  void testMatchesAnyRuleWithNotOneOfRuleNotPassed() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addNotOneOfCondition(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf("value1"));

    Assertions.assertFalse(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }

  @DisplayName("findMatchingRule() with oneOf rule on a string")
  @Test
  void testMatchesAnyRuleWithOneOfRuleOnString() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addOneOfCondition(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf("value1"));

    Assertions.assertTrue(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }

  @DisplayName("findMatchingRule() with oneOf rule on an integer")
  @Test
  void testMatchesAnyRuleWithOneOfRuleOnInteger() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addOneOfConditionWithIntegers(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf(2));

    Assertions.assertTrue(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }

  @DisplayName("findMatchingRule() with oneOf rule on a double")
  @Test
  void testMatchesAnyRuleWithOneOfRuleOnDouble() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addOneOfConditionWithDoubles(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf(1.5));

    Assertions.assertTrue(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }

  @DisplayName("findMatchingRule() with oneOf rule on a boolean")
  @Test
  void testMatchesAnyRuleWithOneOfRuleOnBoolean() {
    List<Rule> rules = new ArrayList<>();
    Rule rule = createRule(new ArrayList<>());
    addOneOfConditionWithBoolean(rule);
    rules.add(rule);

    EppoAttributes subjectAttributes = new EppoAttributes();
    subjectAttributes.put("oneOf", EppoValue.valueOf(true));

    Assertions.assertTrue(RuleValidator.findMatchingRule(subjectAttributes, rules).isPresent());
  }
}
