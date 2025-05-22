package cloud.eppo.util;

import cloud.eppo.Utils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JavaBase64Codec implements Utils.Base64Codec {
  @Override
  public String base64Encode(String input) {
    return new String(Base64.getEncoder().encode(input.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public String base64Decode(String input) {
    byte[] decodedBytes = Base64.getDecoder().decode(input);
    if (decodedBytes.length == 0 && !input.isEmpty()) {
      throw new RuntimeException(
          "zero byte output from Base64; if not running on Android hardware be sure to use RobolectricTestRunner");
    }
    return new String(decodedBytes);
  }
}
