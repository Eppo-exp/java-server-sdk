package com.eppo.sdk.helpers;

import cloud.eppo.rac.exception.InvalidInputException;

/** Input Validator Class */
public class InputValidator {
  /**
   * This function is used to validate input
   *
   * @throws InvalidInputException
   */
  public static boolean validateNotBlank(String input, String errorMsg)
      throws InvalidInputException {
    if (input.trim().isEmpty()) {
      throw new InvalidInputException(errorMsg);
    }

    return true;
  }
}
