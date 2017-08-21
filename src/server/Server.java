package  server;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

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



    public static void main(String args[]) {
        //new Server();

        String serverIP = "127.0.0.1";
        String keyspace = "repl1";
        System.out.println("Attempting ");
        Cluster cluster = Cluster.builder()
                .addContactPoints(serverIP)
                .build();
        System.out.println("Conenction successful");
        Session session = cluster.connect(keyspace);
        String cqlStatement = "SELECT * FROM table1";
        System.out.println("Keyspace successful");

        for (Row row : session.execute(cqlStatement)) {
            System.out.println(row.toString());
        }
    }
    }