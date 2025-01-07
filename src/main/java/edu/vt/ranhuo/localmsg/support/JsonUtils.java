package edu.vt.ranhuo.localmsg.support;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.vt.ranhuo.localmsg.core.Message;

public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static byte[] serialize(Message message) throws Exception {
        return objectMapper.writeValueAsBytes(message);
    }

    public static Message deserialize(byte[] data) throws Exception {
        return objectMapper.readValue(data, Message.class);
    }
}
