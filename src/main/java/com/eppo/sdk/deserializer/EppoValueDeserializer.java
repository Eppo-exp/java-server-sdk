package com.eppo.sdk.deserializer;

import com.eppo.sdk.dto.EppoValue;
import com.eppo.sdk.exception.UnsupportedEppoValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Eppo Value Deserializer Class
 */
public class EppoValueDeserializer extends StdDeserializer<EppoValue> {

    public EppoValueDeserializer() {
        this((Class<?>) null);
    }

    protected EppoValueDeserializer(Class<?> vc) {
        super(vc);
    }

    protected EppoValueDeserializer(JavaType valueType) {
        super(valueType);
    }

    protected EppoValueDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    /**
     * This function is used to deserialize JSON to EppoValue
     *
     * @param jsonParser
     * @param deserializationContext
     * @return
     * @throws IOException
     */
    @Override
    public EppoValue deserialize(
            JsonParser jsonParser,
            DeserializationContext deserializationContext
    ) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        return parseEppoValue(node);
    }

    /**
     * This function is used to parse json node to create Eppo Value
     *
     * @param node
     * @return
     */
    private EppoValue parseEppoValue(JsonNode node) {
        switch (node.getNodeType()) {
            case ARRAY:
                List<String> array = new ArrayList<>();
                if (node.size() == 0) {
                    return EppoValue.valueOf(new ArrayList<>());
                }
                if (node.get(0).getNodeType() != JsonNodeType.STRING) {
                    throw new UnsupportedEppoValue("Unsupported Eppo Values");
                }
                for (int i = 0; i < node.size(); i++) {
                    array.add(node.get(i).asText());
                }
                return EppoValue.valueOf(array);
            case NUMBER:
                return EppoValue.valueOf(node.asDouble());
            case STRING:
                return EppoValue.valueOf(node.asText());
            case BOOLEAN:
                return EppoValue.valueOf(node.asBoolean());
            case OBJECT:
            case POJO:
                return EppoValue.valueOf(node);
            default:
                return EppoValue.nullValue();
        }
    }
}
