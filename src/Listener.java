import protocol.CommunicationHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class listens for incoming client requests
 * and passes them to CommunicationHandler object.
 */
public class Listener extends Thread {

    private int port;
    private boolean stopped;
    private CommunicationHandler communicationHandler;

    /**
     * Constructor.
     *
     * @param port
     * @param communicationHandler
     */
    Listener(int port, CommunicationHandler communicationHandler) {
        this.port = port;
        this.communicationHandler = communicationHandler;
        this.stopped = false;
    }

    @Override
    public void run() {

        try {

            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Listening on port " + port);

            while (!stopped) {
                // accept connection from client
                Socket socket = serverSocket.accept();
                communicationHandler.handleNewClient(socket);
                //CommunicationHandler communicationHandler = new CommunicationHandler();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}