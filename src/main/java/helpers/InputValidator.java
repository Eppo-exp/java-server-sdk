package helpers;

import exception.InvalidInputException;

public class InputValidator {
    /**
     * This function is used to validate input
     * @param input
     * @param errorMsg
     * @return
     * @throws Exception
     */
    public static boolean validateNotBlank(String input, String errorMsg) throws Exception {
        if (input.isBlank()) {
            throw new InvalidInputException(errorMsg);
        }

        return true;
    }
}
