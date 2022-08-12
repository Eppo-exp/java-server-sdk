package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        condition1.value = EppoValue.valueOf(10);
        condition1.attribute = "price";
        condition1.operator = OperatorType.GTE;

        Condition condition2 = new Condition();
        condition2.value = EppoValue.valueOf(20);
        condition2.attribute = "price";
        condition2.operator = OperatorType.LTE;

        addConditionToRule(rule, condition1);
        addConditionToRule(rule, condition2);
    }

    public void addRegexConditionToRule(Rule rule) {
        Condition condition = new Condition();
        condition.value = EppoValue.valueOf("[a-z]+");
        condition.attribute = "match";
        condition.operator = OperatorType.MATCHES;

        addConditionToRule(rule, condition);
    }

    public void addOneOfCondition(Rule rule) {
        Condition condition = new Condition();
        List<String> values = new ArrayList<>();
        values.add("value1");
        values.add("value2");
        condition.value = EppoValue.valueOf(values);
        condition.attribute = "oneOf";
        condition.operator = OperatorType.ONE_OF;

        addConditionToRule(rule, condition);
    }

    public void addNotOneOfCondition(Rule rule) {
        Condition condition = new Condition();
        List<String> values = new ArrayList<>();
        values.add("value1");
        values.add("value2");
        condition.value = EppoValue.valueOf(values);
        condition.attribute = "oneOf";
        condition.operator = OperatorType.NOT_ONE_OF;

        addConditionToRule(rule, condition);
    }

    public void addNameToSubjectAttribute(SubjectAttributes subjectAttributes) {
        subjectAttributes.put("name", EppoValue.valueOf("test"));
    }

    public void addPriceToSubjectAttribute(SubjectAttributes subjectAttributes) {
        subjectAttributes.put("price", EppoValue.valueOf("30"));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with empty conditions")
    @Test
    void testMatchesAnyRuleWithEmptyConditions() {
        List<Rule> rules = new ArrayList<>();
        final Rule ruleWithEmptyConditions = createRule(new ArrayList<>());
        rules.add(ruleWithEmptyConditions);
        SubjectAttributes subjectAttributes = new SubjectAttributes();
        addNameToSubjectAttribute(subjectAttributes);

        Assertions.assertTrue(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with empty rules")
    @Test
    void testMatchesAnyRuleWithEmptyRules() {
        List<Rule> rules = new ArrayList<>();
        SubjectAttributes subjectAttributes = new SubjectAttributes();
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

        SubjectAttributes subjectAttributes = new SubjectAttributes();
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

        SubjectAttributes subjectAttributes = new SubjectAttributes();
        subjectAttributes.put("price", EppoValue.valueOf(15));

        Assertions.assertTrue(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() throw InvalidSubjectAttribute")
    @Test
    void testMatchesAnyRuleWhenThrowInvalidSubjectAttribute() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addNumericConditionToRule(rule);
        rules.add(rule);

        SubjectAttributes subjectAttributes = new SubjectAttributes();
        subjectAttributes.put("price", EppoValue.valueOf("abcd"));

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

        SubjectAttributes subjectAttributes = new SubjectAttributes();
        subjectAttributes.put("match", EppoValue.valueOf("abcd"));

        Assertions.assertTrue(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with regex condition not matched")
    @Test
    void testMatchesAnyRuleWithRegexConditionNotMatched() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addRegexConditionToRule(rule);
        rules.add(rule);

        SubjectAttributes subjectAttributes = new SubjectAttributes();
        subjectAttributes.put("match", EppoValue.valueOf("123"));

        Assertions.assertFalse(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with not oneOf rule")
    @Test
    void testMatchesAnyRuleWithNotOneOfRule() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addNotOneOfCondition(rule);
        rules.add(rule);

        SubjectAttributes subjectAttributes = new SubjectAttributes();
        subjectAttributes.put("oneOf", EppoValue.valueOf("value3"));

        Assertions.assertTrue(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

    @DisplayName("Text RuleValidator.matchesAnyRule() with not oneOf rule not passed")
    @Test
    void testMatchesAnyRuleWithNotOneOfRuleNotPassed() {
        List<Rule> rules = new ArrayList<>();
        Rule rule = createRule(new ArrayList<>());
        addNotOneOfCondition(rule);
        rules.add(rule);

        SubjectAttributes subjectAttributes = new SubjectAttributes();
        subjectAttributes.put("oneOf", EppoValue.valueOf("value1"));

        Assertions.assertFalse(RuleValidator.matchesAnyRule(subjectAttributes, rules));
    }

}