package  server;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    Session session;
    Cluster cluster;

    class ClientListener extends Thread {

        public void run(){
            try{
                ServerSocket serverSocket=new ServerSocket(12345);
                while(true){
                    Socket socket=serverSocket.accept();
                    DataInputStream  in =new DataInputStream(socket.getInputStream());
                    DataOutputStream out=new DataOutputStream(socket.getOutputStream());
                    String query=in.readUTF();
                    System.out.println("Server received:"+query);
                    for (Row row : session.execute(query)) {
                        out.writeUTF(row.toString());
                    }
                    socket.close();
                }
            }catch(Exception exception){

            }

        }
    }

    Server(){
        String serverIP = "127.0.0.1";
        String keyspace = "repl1";
        cluster = Cluster.builder()
                .addContactPoints(serverIP)
                .build();
        session = cluster.connect(keyspace);
        new ClientListener().run();

    }



    public static void main(String args[]) {
        new Server();

//        String serverIP = "127.0.0.1";
//        String keyspace = "repl1";
//        System.out.println("Attempting ");
//        Cluster cluster = Cluster.builder()
//                .addContactPoints(serverIP)
//                .build();
//        System.out.println("Conenction successful");
//        Session session = cluster.connect(keyspace);
//        String cqlStatement = "SELECT * FROM table1";
//        System.out.println("Keyspace mesuccessful");
//
//        for (Row row : session.execute(cqlStatement)) {
//            System.out.println(row.toString());
//        }
    }
    }