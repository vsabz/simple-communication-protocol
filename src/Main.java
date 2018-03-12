import protocol.CommunicationHandler;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        if (args.length != 2)
            throw new IllegalArgumentException("Please provide port number and hostname for this instance.");

        String port, hostname;
        // get IP/port from args
        port = args[0];
        hostname = args[1];
        System.out.println("Hi, I am " + hostname + ".");

        // create communication handler and start listening
        CommunicationHandler communicationHandler = new CommunicationHandler(hostname);
        Listener listener = new Listener(Integer.parseInt(port),communicationHandler);
        listener.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // read from input and attempt to init a connection towards peer (CONTACT_HEADER)
        System.out.println("Enter 'connect [remote port number]' to connect to peer, or wait for connection request.");
        System.out.println("Type 'exit' to terminate the program.");

        Scanner scanner = new Scanner(System.in);
        String commandFromClient = scanner.next().toLowerCase();

        while (!commandFromClient.toLowerCase().equals("exit")) {
            // stop listening in case of connecting to peer
            if (commandFromClient.equals("connect"))
                listener.setStopped(true);
            // if disconnected start listening for incoming requests
            else if (commandFromClient.equals("disconnect")) {
                listener = new Listener(Integer.parseInt(port),communicationHandler);
                listener.start();
            }
            // pass command to handler
            communicationHandler.handleCommandFromClient(commandFromClient);
            commandFromClient = scanner.next().toLowerCase();

        }

        listener.setStopped(true);


        //System.out.println(commandFromClient);


    }
}
