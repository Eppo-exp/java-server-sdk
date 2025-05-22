package cloud.eppo.dto.adapters;

import cloud.eppo.api.EppoValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class EppoValueSerializer extends StdSerializer<EppoValue> {
  protected EppoValueSerializer(Class<EppoValue> t) {
    super(t);
  }

  public EppoValueSerializer() {
    this(null);
  }

  @Override
  public void serialize(EppoValue src, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    if (src.isBoolean()) {
      jgen.writeBoolean(src.booleanValue());
    }
    if (src.isNumeric()) {
      jgen.writeNumber(src.doubleValue());
    }
    if (src.isString()) {
      jgen.writeString(src.stringValue());
    }
    if (src.isStringArray()) {
      String[] arr = src.stringArrayValue().toArray(new String[0]);
      jgen.writeArray(arr, 0, arr.length);
    } else {
      jgen.writeNull();
    }
  }
}
