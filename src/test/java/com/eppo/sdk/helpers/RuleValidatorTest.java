package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.Condition;
import com.eppo.sdk.dto.OperatorType;
import com.eppo.sdk.dto.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RuleValidatorTest {

    public Rule createRule(List<Condition> conditions) {
        final Rule rule = new Rule();
        rule.conditions = conditions;
        return rule;
    }

    public void addConditionToRule(Rule rule, Condition condition) {
        rule.conditions.add(condition);
    }

    public void addNumericConditionToRule(Rule rule) {
        Condition condition1 = new Condition();
        condition1.value = "10";
        condition1.attribute = "price";
        condition1.operator = OperatorType.GTE;

        Condition condition2 = new Condition();
        condition2.value = "20";
        condition2.attribute = "price";
        condition2.operator = OperatorType.LTE;

        addConditionToRule(rule, condition1);
        addConditionToRule(rule, condition2);
    }

    public void addRegexConditionToRule(Rule rule) {
        Condition condition = new Condition();
        condition.value = "[a-z]+";
        condition.attribute = "match";
        condition.operator = OperatorType.MATCHES;

        addConditionToRule(rule, condition);
    }

    public void addOneOfCondition(Rule rule) {
        Condition condition = new Condition();
        condition.value = "[\"value1\", \"value2\"]";
        condition.attribute = "oneOf";
        condition.operator = OperatorType.ONE_OF;

        addConditionToRule(rule, condition);
    }

    public void addNotOneOfCondition(Rule rule) {
        Condition condition = new Condition();
        condition.value = "[\"value1\", \"value2\"]";
        condition.attribute = "oneOf";
        condition.operator = OperatorType.NOT_ONE_OF;

        addConditionToRule(rule, condition);
    }

    public void addNameToSubjectAttribute(Map<String, String> subjectAttributes) {
        subjectAttributes.put("name", "test");
    }

    public void addPriceToSubjectAttribute(Map<String, String> subjectAttributes) {
        subjectAttributes.put("price", "30");
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with empty conditions")
    @Test
    void testMatchesAnyRuleWithEmptyConditions() {
        List<Rule> rules = new ArrayList<>();
        final Rule ruleWithEmptyConditions = createRule(new ArrayList<>());
        rules.add(ruleWithEmptyConditions);
        Map<String, String> subjectAttributes = new HashMap<>();
        addNameToSubjectAttribute(subjectAttributes);

        Assertions.assertTrue(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with empty rules")
    @Test
    void testMatchesAnyRuleWithEmptyRules() {
        List<Rule> rules = new ArrayList<>();
        Map<String, String> subjectAttributes = new HashMap<>();
        addNameToSubjectAttribute(subjectAttributes);

        Assertions.assertFalse(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() when no rule matches")
    @Test
    void testMatchesAnyRuleWhenNoRuleMatches() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addNumericConditionToRule(rule);
        rules.add(rule);

        Map<String, String> subjectAttributes = new HashMap<>();
        addPriceToSubjectAttribute(subjectAttributes);

        Assertions.assertFalse(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() when rule matches")
    @Test
    void testMatchesAnyRuleWhenRuleMatches() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addNumericConditionToRule(rule);
        rules.add(rule);

        Map<String, String> subjectAttributes = new HashMap<>();
        subjectAttributes.put("price", "15");

        Assertions.assertTrue(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() throw InvalidSubjectAttribute")
    @Test
    void testMatchesAnyRuleWhenThrowInvalidSubjectAttribute() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addNumericConditionToRule(rule);
        rules.add(rule);

        Map<String, String> subjectAttributes = new HashMap<>();
        subjectAttributes.put("price", "abcd");

        Assertions.assertThrows(
                RuntimeException.class,
                () -> RuleValidator.matchesAnyRule(subjectAttributes, rules),
                "Invalid subject attribute : abcd"
        );
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with regex condition")
    @Test
    void testMatchesAnyRuleWithRegexCondition() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addRegexConditionToRule(rule);
        rules.add(rule);

        Map<String, String> subjectAttributes = new HashMap<>();
        subjectAttributes.put("match", "abcd");

        Assertions.assertTrue(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with regex condition not matched")
    @Test
    void testMatchesAnyRuleWithRegexConditionNotMatched() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addRegexConditionToRule(rule);
        rules.add(rule);

        Map<String, String> subjectAttributes = new HashMap<>();
        subjectAttributes.put("match", "1223");

        Assertions.assertFalse(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with not oneOf rule")
    @Test
    void testMatchesAnyRuleWithNotOneOfRule() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addNotOneOfCondition(rule);
        rules.add(rule);

        Map<String, String> subjectAttributes = new HashMap<>();
        subjectAttributes.put("oneOf", "value3");

        Assertions.assertTrue(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with not oneOf rule not passed")
    @Test
    void testMatchesAnyRuleWithNotOneOfRuleNotPassed() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addNotOneOfCondition(rule);
        rules.add(rule);

        Map<String, String> subjectAttributes = new HashMap<>();
        subjectAttributes.put("oneOf", "value1");

        Assertions.assertFalse(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

}