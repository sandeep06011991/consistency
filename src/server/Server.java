package  server;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {

    class ClientListener extends Thread {


        public void run(){
            try{
                ServerSocket serverSocket=new ServerSocket(12345);
                while(true){
                    Socket socket=serverSocket.accept();
                    DataInputStream  in =new DataInputStream(socket.getInputStream());
                    System.out.println("Server received"+in.readUTF());
                    System.out.println("Server received"+in.readUTF());

                    Log.info("Log Testing ");
                }
            }catch(Exception exception){

            }

        }
    }

    Server(){
        new ClientListener().run();
    }

    public static void main(String args[]) throws Exception{
        new Server();
        }
    }