package protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * This class defines protocol message.
 *
 */
public class Message {

    // message types
    protected enum Type {
        CONTACT_HEADER, TRANSFER_START, TRANSFER_BYTES, TRANSFER_END, TRANSFER_ACK, CLOSE_CONTACT, UNDEFINED
    }

    private String payload;
    private Type type;

    /**
     * Constructor.
     *
     * @param type type of message
     * @param payload payload of message
     */
    Message (Type type, String payload) {
        this.payload = payload;
        this.type = type;
    }

    /**
     * Constructor.
     * Builds a Message object by deserializing it from received byte array.
     * @param msg
     */
    Message (byte[] msg) {
        short receivedMessageType = ByteBuffer.wrap(Arrays.copyOfRange(msg, 0, 2)).order(ByteOrder.BIG_ENDIAN).getShort();
        short length = ByteBuffer.wrap(Arrays.copyOfRange(msg, 2, 4)).order(ByteOrder.BIG_ENDIAN).getShort();

        switch (receivedMessageType) {
            case 0x01:
                this.type = Type.CONTACT_HEADER;
                this.payload = new String(Arrays.copyOfRange(msg, 4, 4+length), Charset.forName("UTF-8"));
                break;
            case 0x02:
                this.type = Type.TRANSFER_START;
                this.payload = String.valueOf(ByteBuffer.allocate(Long.BYTES).put(Arrays.copyOfRange(msg, 4, 4+length)).flip().getLong());
                break;
            case 0x03:
                this.type = Type.TRANSFER_BYTES;
                this.payload = new String(Arrays.copyOfRange(msg, 4, 4+length));
                break;
            case 0x04:
                this.type = Type.TRANSFER_END;
                this.payload = null;
                break;
            case 0x05:
                this.type = Type.TRANSFER_ACK;
                this.payload = null;
                break;
            case 0x06:
                this.type = Type.CLOSE_CONTACT;
                this.payload = null;
                break;
        }
    }

    /**
     * Serializes message and return byte array.
     *
     * @return
     */
    protected byte[] serialize() {
        byte[] serializedPayload = new byte[0];
        ByteBuffer serializedType = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN);
        ByteBuffer serializedLength = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN);

        switch (type) {
            case CONTACT_HEADER:
                serializedType = serializedType.putShort((short)1);
                serializedPayload = payload.getBytes(Charset.forName("UTF-8"));
                serializedLength = serializedLength.putShort((short) serializedPayload.length);
                //System.out.println("LENGTH:" + serializedPayload.length);
                break;

            case TRANSFER_START:
                serializedType = serializedType.putShort((short)2);
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putLong(Long.valueOf(payload));
                serializedPayload = buffer.array();
                serializedLength = serializedLength.putShort((short)8);
                break;

            case TRANSFER_BYTES:
                serializedPayload = payload.getBytes();
                serializedLength = serializedLength.putShort((short) serializedPayload.length);
                serializedType = serializedType.putShort((short)3);
                break;

            case TRANSFER_END:
                serializedType = serializedType.putShort((short)4);
                serializedLength = serializedLength.putShort((short)0);
                break;

            case TRANSFER_ACK:
                serializedType = serializedType.putShort((short)5);
                serializedLength = serializedLength.putShort((short)0);
                break;

            case CLOSE_CONTACT:
                serializedType = serializedType.putShort((short)6);
                serializedLength = serializedLength.putShort((short)0);
                break;

        }


        // put everything together as 1 message
        byte[] serializedMessage = new byte[serializedPayload.length + 4];
        System.arraycopy(serializedType.array(), 0, serializedMessage, 0, 2);
        System.arraycopy(serializedLength.array(), 0, serializedMessage, 2, 2);
        System.arraycopy(serializedPayload, 0, serializedMessage,4, serializedPayload.length);

        return serializedMessage;
    }

    public String getPayload() {
        return payload;
    }

    public Type getType() {
        return type;
    }

    public String toString() {
        return "Message Type : " + type + "; Payload : " + payload;
    }
}
