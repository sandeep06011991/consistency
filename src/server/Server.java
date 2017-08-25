package  server;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.SyntaxError;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    Session session;
    Cluster cluster;
    int node_id;
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
                    try{
                        for (Row row : session.execute(query)) {
                            out.writeUTF(row.toString());
                        }
                    }catch (SyntaxError se){
                        out.writeUTF("Syntax Error.Try again");
                    }
                    socket.close();
                }
            }catch(Exception exception){
                System.out.print("Something wierd happenned");
            }

        }
    }

    Server(int node_id){
        this.node_id=node_id;
        String serverIP = Constants.getHostId(node_id);
        String keyspace = Constants.getKeySpace();
        cluster = Cluster.builder()
                .addContactPoints(serverIP)
                .build();
        session = cluster.connect(keyspace);
        new ClientListener().run();

    }



    public static void main(String args[]) {
        if(args.length!=1){
            System.out.print("run as Server ");
        }
        int node_id=Integer.parseInt(args[0]);
        new Server(node_id);

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