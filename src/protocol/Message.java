package protocol;

import javax.sound.midi.ShortMessage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Message {

    protected enum Type {
        CONTACT_HEADER, TRANSFER_START, TRANSFER_BYTES, TRANSFER_END, TRANSFER_ACK, CLOSE_CONTACT
    }

    private String payload;
    private Type type;
    private byte[] message;

    Message (Type type, String payload) {
        this.payload = payload;
        this.type = type;
    }

    Message (byte[] message) {
        short receivedMessageType = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 2)).order(ByteOrder.BIG_ENDIAN).getShort();
        short length = ByteBuffer.wrap(Arrays.copyOfRange(message, 2, 4)).order(ByteOrder.BIG_ENDIAN).getShort();

        switch (receivedMessageType) {
            case 0x01:
                this.type = Type.CONTACT_HEADER;
                this.payload = new String(Arrays.copyOfRange(message, 4, length), Charset.forName("UTF-8"));
                break;
            case 0x02:
                this.type = Type.TRANSFER_START;
                break;
            case 0x03:
                this.type = Type.TRANSFER_BYTES;
                break;
            case 0x04:
                this.type = Type.TRANSFER_END;
                break;
            case 0x05:
                this.type = Type.TRANSFER_ACK;
                break;
            case 0x06:
                this.type = Type.CLOSE_CONTACT;
                break;
        }

        this.message = message;
    }

    protected byte[] serialize() {
        // TODO
        byte[] serializedPayload = new byte[0];
        ByteBuffer serializedType = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN);
        ByteBuffer serializedLength = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN);

        switch (type) {
            case CONTACT_HEADER:
                serializedType = serializedType.putShort((short)1);
                serializedPayload = payload.getBytes(Charset.forName("UTF-8"));
                serializedLength = serializedLength.putShort((short) serializedPayload.length);
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
        System.arraycopy(serializedType, 0, serializedMessage, 0, 2);
        System.arraycopy(serializedLength, 0, serializedMessage, 2, 2);
        System.arraycopy(serializedPayload, 0, serializedMessage,4, serializedPayload.length);


        return serializedMessage;
    }

    /*protected Message deserialize() {
        // TODO
        return this;
    }*/

    public String getPayload() {
        return payload;
    }

    public Type getType() {
        return type;
    }
}
