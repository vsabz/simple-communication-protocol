import protocol.CommunicationHandler;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 3)
            throw new IllegalArgumentException("Please provide port number, hostname for this instance and MTU.");

        // get IP, port and MTU from args
        String port, hostname, mtu;
        port = args[0];
        hostname = args[1];
        mtu = args[2];

        if (Integer.parseInt(mtu) < 5)
            throw new IllegalArgumentException("MTU should be at least 5, since protocol's 'type' and 'length' field are 4 bytes.");
        System.out.println("Hi, I am " + hostname + ".");

        // create communication handler and start listening
        CommunicationHandler communicationHandler = new CommunicationHandler(hostname, Integer.parseInt(mtu)  );
        Listener listener = new Listener(Integer.parseInt(port),communicationHandler);
        listener.start();

        Thread.sleep(100);

        // read from input and attempt to init a connection towards peer (CONTACT_HEADER)
        System.out.println("Use following commands to interact with the program.");
        System.out.println("'connect [ip:port]' to connect to peer.");
        System.out.println("'send [your_message]' to send message to peer.");
        System.out.println("'disconnect' to disconnect from the peer.");

        // read commands from the client
        Scanner scanner = new Scanner(System.in);
        String commandFromClient = scanner.nextLine().toLowerCase();
        String command = commandFromClient.split(" ")[0];
        String argument = null;
        if (commandFromClient.split(" ").length > 1)
            argument = commandFromClient.split(" ")[1];

        while (true) {

            // pass command to handler
            communicationHandler.handleCommandFromConsoleUser(command, argument);

            // read next line
            commandFromClient = scanner.nextLine();
            //commandFromClient = bufferedReader.readLine().toLowerCase();
            command = commandFromClient.split(" ")[0].toLowerCase();
            argument = null;
            if (commandFromClient.split(" ").length > 1)
                argument = commandFromClient.split(" ")[1];
        }

    }
}
