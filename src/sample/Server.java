package sample;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private int MAX_CLIENTS = 100;
    private volatile Set<Socket> channel = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private SignalDispatcher reader;
    private Handler connectionHandler;
    private ServerSocket ss = null;
    private int nextID;


    public Server() throws IOException {
        System.out.println("Run Server");
        ss = new ServerSocket(9000, MAX_CLIENTS);
        reader = new SignalDispatcher();
        connectionHandler = new Handler();
        reader.start();
        connectionHandler.start();

    }

    public void notifyAll(final byte signal, byte answer) throws IOException {
        final byte[] args = new byte[]{signal, answer};
        for (Socket s : channel) {
            response(s.getOutputStream(), args);
        }
        System.out.println("Notify ALL " + signal);
    }

    private void response(OutputStream os, byte... args) throws IOException {
        if (args[0] == Signal.GET_ID) {
            args[1] = (byte) nextID++;
            os.write(args);
            System.out.println("Response " + nextID);
        } else if (args[0] == Signal.GET_USER_COUNT) {
            args[1] = (byte) channel.size();
            os.write(args);
            System.out.println("Response " + channel.size());
        } else if (args[0] == Signal.CHANGE_USER_COUNT){
            os.write(args);
            System.out.println("Add user");
        }
    }

    private class SignalDispatcher extends Thread {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                for (Socket s : channel) {
                    try {
                        InputStream is = s.getInputStream();
                        while (is.available() > 0) {
                            byte[] res = new byte[2];
                            is.read(res);
                            System.out.println("Request " + res[0]);
                            response(s.getOutputStream(), res);
                        }
                    } catch (IOException e) {
                        System.err.println(s.getInetAddress().getHostAddress() + "error " + e.getMessage());
                    }
                }
            }
        }
    }

    private class Handler extends Thread {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Socket s = ss.accept();
                    s.setTcpNoDelay(true);
                    channel.add(s);
                    Server.this.notifyAll(Signal.CHANGE_USER_COUNT, (byte) 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getConnectionsCount() {
        return channel.size();
    }

    public interface Signal {
        byte GET_ID = 0;
        byte GET_USER_COUNT = 1;
        byte CHANGE_USER_COUNT = 2;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = new Server();

        while (true) {
            Thread.sleep(5_000);
            System.out.println("Connected " + server.getConnectionsCount() + " users");
        }
    }


}
