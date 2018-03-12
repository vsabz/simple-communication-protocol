package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class CommunicationHandler extends Thread {

    private Socket clientSocket;
    private boolean stopped;
    private String hostname;
    private String remoteHostname;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    public CommunicationHandler(String hostname) {
        this.stopped = false;
        this.hostname = hostname;
    }

    public void handleNewClient(Socket socket) throws IOException {
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
        while (!stopped) {
            // start reading from socket
            int length = dataInputStream.readInt();
            if (length>0){
                byte[] 
            }
        }
    }

    private void handleMessageFromPeer(byte[] receivedMessage) {
        Message message = new Message(receivedMessage);
        // TODO
    }

    public void handleCommandFromClient(String command, String arg) throws IOException {
        switch (command) {
            case "connect":
                // establish TCP connection
                clientSocket = new Socket("127.0.0.1", Integer.parseInt(arg));
                dataInputStream = new DataInputStream(clientSocket.getInputStream());
                dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

                // send CONTACT_HEADER
                byte[] messageToBeSent = new Message(Message.Type.CONTACT_HEADER, hostname).serialize();
                dataOutputStream.writeInt(messageToBeSent.length);
                dataOutputStream.write(messageToBeSent);

                System.out.println("CONTACT_HEADER sent.");

                break;
            case "disconnect":
                // send CLOSE_CONTACT
                // close TCP connection
                break;
            case "send":
                // send TRANSFER* messages
                break;
        }
    }

}
