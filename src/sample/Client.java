package sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {

    private Socket channel;
    private SignalDispatcher dispatcher;
    private volatile byte n = 0;
    private volatile byte myID = -1;


    public Client() throws IOException {
        System.out.println("Run Client");
        channel = new Socket("localhost",9000);
        channel.setTcpNoDelay(true);
        dispatcher = new SignalDispatcher();
        dispatcher.start();
    }


    public void request(byte signal) throws IOException {
        System.out.println("Request: "+ signal);
        OutputStream os = channel.getOutputStream();
        os.write(new byte[]{signal,0});
    }

    public interface Signal{
        byte GET_ID = 0;
        byte GET_USER_COUNT = 1;
        byte CHANGE_USER_COUNT = 2;
    }

    private class SignalDispatcher extends Thread{
        @Override
        public void run() {
            while(!Thread.interrupted()){
                try {
                    InputStream is = channel.getInputStream();
                    byte[] res = new byte[2];
                    if(is.available()==0)  continue;
                    is.read(res);
                    if(res[0] == Signal.CHANGE_USER_COUNT){
                        n++;
                        System.out.println("New user connected");
                    }else if(res[0]==Signal.GET_ID){
                        myID = res[1];
                        System.out.println("My Id changed "+ myID);
                    }else if(res[0] == Signal.GET_USER_COUNT){
                        n = res[1];
                        System.out.println("Users on server now:" + n);
                    }
                } catch (IOException e) {
                    System.err.println("Dispatcher error " + e.getMessage());
                }
            }
        }
    }

    public byte getCount(){
        return n;
    }

    public byte getMyID(){
        return myID;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
	    Client client = new Client();

        client.request(Signal.GET_ID);
        client.request(Signal.GET_USER_COUNT);

        while(true){
            Thread.sleep(5_000);
            System.out.print("N = " + client.getCount());
            System.out.println(" my ID = " + client.getMyID());
        }


    }

}
