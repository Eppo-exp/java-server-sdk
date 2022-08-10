package helpers;

import exception.InvalidInputException;

public class InputValidator {
    public static boolean validateNotBlank(String input, String errorMsg) throws Exception {
        if (input.isBlank()) {
            throw new InvalidInputException(errorMsg);
        }

        return true;
    }
}
