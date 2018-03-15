package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * This class handles all the communication between 2 instances.
 */
public class CommunicationHandler extends Thread {

    private Socket clientSocket;
    private boolean stopped;
    private String hostname;
    private String remoteHostname;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private int mtu;

    // variables to control semantics of the protocol
    private boolean isContactHeaderSent;
    private boolean isContactHeaderReceived;
    private boolean isConnected;
    private boolean isTransferComplete;
    private long expectedNumberOfBytes;
    private String fullyReceivedText;

    /**
     * Constructor.
     *
     * @param hostname
     * @param mtu
     */
    public CommunicationHandler(String hostname, int mtu) {
        this.stopped = false;
        this.hostname = hostname;
        this.isContactHeaderSent = false;
        this.isContactHeaderReceived = false;
        this.isConnected = false;
        this.mtu = mtu;
        this.isTransferComplete = true;
        this.expectedNumberOfBytes = 0;
        this.fullyReceivedText = "";
    }

    /**
     * Starts handling of newly connected client.
     * Called from the Listener object.
     *
     * @param socket
     * @throws IOException
     */
    public void handleNewClient(Socket socket) throws IOException {
        System.out.println("Client request accepted.");
        clientSocket = socket;
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());

        stopped = false;
        isTransferComplete = true;
        expectedNumberOfBytes = 0;
        fullyReceivedText = "";


        Message.Type receivedType = Message.Type.UNDEFINED;

        // listen for packets
        while (!stopped) {

            // start reading from socket
            if (!receivedType.equals(Message.Type.CLOSE_CONTACT)) {
                int length = dataInputStream.readInt();
                if (length > 0) {
                    byte[] receivedMessage = new byte[length];
                    dataInputStream.readFully(receivedMessage, 0, receivedMessage.length);
                    receivedType = handleMessageFromPeer(receivedMessage);
                }
            } else
                stopped = true;
        }
    }

    /**
     * This method handles incoming messages from the remote peer.
     *
     * It responds to remote peer in accordance to defined specifications here:
     * http://www.cm.in.tum.de/en/open-thesis-topics-guided-research/msc-topic-quic-transport-protocol-implementation-in-java/
     *
     * @param receivedMessage
     * @return
     * @throws IOException
     */
    private Message.Type handleMessageFromPeer(byte[] receivedMessage) throws IOException {

        Message message = new Message(receivedMessage);
        byte[] messageToBeSent;

        // act depending on received message type
        switch (message.getType()) {
            case CONTACT_HEADER:
                System.out.println("** Received CONTACT_HEADER message from " + message.getPayload() + " **");
                isContactHeaderReceived = true;
                isTransferComplete = true;
                fullyReceivedText = "";

                // send CONTACT_HEADER
                if (!isContactHeaderSent) {
                    messageToBeSent = new Message(Message.Type.CONTACT_HEADER, hostname).serialize();
                    dataOutputStream.writeInt(messageToBeSent.length);
                    dataOutputStream.write(messageToBeSent);
                    isContactHeaderSent = true;
                    System.out.println("** CONTACT_HEADER reply sent. **");
                }
                return Message.Type.CONTACT_HEADER;

            case TRANSFER_START:
                System.out.println("** Received TRANSFER_START message. **");
                System.out.println(message.getPayload() + " bytes expected.");

                if (!isTransferComplete)
                    throw new IllegalStateException("Client started new transfer without completing existing one.");

                isTransferComplete = false;
                expectedNumberOfBytes = Long.valueOf(message.getPayload());
                fullyReceivedText = "";
                return Message.Type.TRANSFER_START;

            case TRANSFER_BYTES:
                System.out.println("** Received TRANSFER_BYTES message. **");

                fullyReceivedText += message.getPayload();
                expectedNumberOfBytes -= receivedMessage.length - 4;
                if (expectedNumberOfBytes <= 0)
                    isTransferComplete = true;

                return Message.Type.TRANSFER_BYTES;

            case TRANSFER_END:
                System.out.println("** Received TRANSFER_END message. **");


                // send ACK if all expected bytes are received.
                if (isTransferComplete) {
                    messageToBeSent = new Message(Message.Type.TRANSFER_ACK, null).serialize();
                    dataOutputStream.writeInt(messageToBeSent.length);
                    dataOutputStream.write(messageToBeSent);
                    System.out.println("Transfer finished; received text: " + fullyReceivedText);
                    System.out.println("** Sent TRANSFER_ACK message. **");
                } else
                    throw new IllegalStateException("Client sent TRANSFER_END without completing the transfer.");

                return Message.Type.TRANSFER_END;
            case TRANSFER_ACK:
                System.out.println("** Received TRANSFER_ACK message. **");

                return Message.Type.TRANSFER_ACK;
            case CLOSE_CONTACT:
                System.out.println("** Received CLOSE_CONTACT message, tearing down TCP connection. **");
                dataInputStream.close();
                dataOutputStream.close();
                clientSocket.close();
                isContactHeaderReceived = false;
                isContactHeaderSent = false;
                isTransferComplete = true;
                isConnected = false;


                return Message.Type.CLOSE_CONTACT;
        }

        return Message.Type.UNDEFINED;
    }

    /**
     * This method handles commands from console user.
     *
     * @param command
     * @param arg
     * @throws IOException
     */
    public void handleCommandFromConsoleUser(String command, String arg) throws IOException {
        byte[] messageToBeSent;
        Message.Type receivedType;
        int length;

        // act depending on received command
        switch (command) {
            // connect to remote peer
            case "connect":

                // extract ip and port
                String ip = "127.0.0.1";
                int port = 0;
                if (arg.split(":").length == 2) {
                    ip = arg.split(":")[0];
                    port = Integer.parseInt(arg.split(":")[1]);
                } else
                    port = Integer.parseInt(arg);

                // establish TCP connection
                clientSocket = new Socket(ip, port);
                dataInputStream = new DataInputStream(clientSocket.getInputStream());
                dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

                // send CONTACT_HEADER
                messageToBeSent = new Message(Message.Type.CONTACT_HEADER, hostname).serialize();
                dataOutputStream.writeInt(messageToBeSent.length);
                dataOutputStream.write(messageToBeSent);
                isContactHeaderSent = true;
                System.out.println("** CONTACT_HEADER sent. **");
                System.out.println("** Waiting for CONTACT_HEADER reply. **");

                stopped = false;
                isConnected = true;

                // wait for CONTACT_HEADER reply
                receivedType = Message.Type.UNDEFINED;
                length = 0;
                length = dataInputStream.readInt();
                if (length > 0) {
                    byte[] receivedMessage = new byte[length];
                    dataInputStream.readFully(receivedMessage, 0, receivedMessage.length);
                    receivedType = handleMessageFromPeer(receivedMessage);
                }

                if (!receivedType.equals(Message.Type.CONTACT_HEADER))
                    throw new IllegalStateException("First message from the peer is not CONTACT_HEADER.");
                else
                    isContactHeaderReceived = true;

                break;

            // disconnect from remote peer
            case "disconnect":
                if (!isConnected) {
                    System.out.println("Not connected. Cannot disconnect.");
                    break;
                }
                // send CLOSE_CONTACT
                messageToBeSent = new Message(Message.Type.CLOSE_CONTACT, null).serialize();
                dataOutputStream.writeInt(messageToBeSent.length);
                dataOutputStream.write(messageToBeSent);
                isContactHeaderReceived = false;
                isContactHeaderSent = false;
                isConnected = false;
                stopped = true;

                break;

            // send text to remote peer
            case "send":
                if (!isConnected) {
                    System.out.println("Not connected. Cannot send a message.");
                    break;
                }

                if (!isContactHeaderSent || !isContactHeaderReceived) {
                    System.out.println("");
                }

                // send TRANSFER_START messages
                System.out.println(arg);
                messageToBeSent = new Message(Message.Type.TRANSFER_START, String.valueOf(arg.getBytes().length)).serialize();
                dataOutputStream.writeInt(messageToBeSent.length);
                dataOutputStream.write(messageToBeSent);
                System.out.println("** Sent TRANSFER_START message. **");

                //send TRANSFER_BYTES messages
                byte[][] chunkedArray = chunkArray(arg.getBytes(), mtu - 4);
                for (int i=0; i<chunkedArray.length; i++) {
                    messageToBeSent = new Message(Message.Type.TRANSFER_BYTES, new String(chunkedArray[i])).serialize();
                    //System.out.println(new String(chunkedArray[i]));
                    dataOutputStream.writeInt(messageToBeSent.length);
                    dataOutputStream.write(messageToBeSent);
                    System.out.println("** Sent TRANSFER_BYTES message: " + chunkedArray[i].length + "-byte payload & 4-byte header. ** ");
                }

                //send TRANSFER_END messages
                messageToBeSent = new Message(Message.Type.TRANSFER_END, null).serialize();
                dataOutputStream.writeInt(messageToBeSent.length);
                dataOutputStream.write(messageToBeSent);
                System.out.println("** Sent TRANSFER_END message. **");

                System.out.println("Waiting for TRANSFER_ACK message.");

                // wait for CONTACT_HEADER reply
                receivedType = Message.Type.UNDEFINED;
                length = 0;
                length = dataInputStream.readInt();
                if (length > 0) {
                    byte[] receivedMessage = new byte[length];
                    dataInputStream.readFully(receivedMessage, 0, receivedMessage.length);
                    receivedType = handleMessageFromPeer(receivedMessage);
                }

                if (!receivedType.equals(Message.Type.TRANSFER_ACK))
                    throw new IllegalStateException("Did not get ack from the peer.");

                break;
        }
    }


    /**
     * Code to divide array into eqaully-sized chunks.
     *
     * Code from: https://gist.github.com/lesleh/7724554
     * @param array
     * @param chunkSize
     * @return
     */
    public static byte[][] chunkArray(byte[] array, int chunkSize) {
        int numOfChunks = (int)Math.ceil((double)array.length / chunkSize);
        byte[][] output = new byte[numOfChunks][];

        for(int i = 0; i < numOfChunks; ++i) {
            int start = i * chunkSize;
            int length = Math.min(array.length - start, chunkSize);

            byte[] temp = new byte[length];
            System.arraycopy(array, start, temp, 0, length);
            output[i] = temp;
        }

        return output;
    }

}
