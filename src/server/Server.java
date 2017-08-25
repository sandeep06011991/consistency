package  server;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.SyntaxError;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class Server {
    Session session;
    Cluster cluster;
    int ssIP;
    int node_id;
    int lamport_clock;
    int number_of_servers;
//    Lock lock; Worry about this later
    int pending_proposal=0;
    Map<Integer,String> awaiting_approval=new HashMap<Integer, String>();
    Map<Integer,Integer> acks=new HashMap<Integer, Integer>();


    void send_query_to_server(int server_port,Integer lamport_clock,String query){
        try {
            Socket client = new Socket("127.0.0.1",server_port);
            DataOutputStream out=new DataOutputStream(client.getOutputStream());
            out.writeUTF(lamport_clock+"$"+query);
            client.close();
        }catch (IOException e){
            System.out.println("Finished reading");
        }

    }

    void multiCastWriteQuery(String query){
        lamport_clock=lamport_clock+10;
        pending_proposal=lamport_clock;
        Integer lamp_clock=new Integer(lamport_clock);
        awaiting_approval.put(lamp_clock,query);
        acks.put(lamp_clock,new Integer(1));
        //Self approval
        //Put Query in Queue and Send out Multicast
        for(int server_port:Constants.SERVER_SERVER){
            // Only do this for servers other than me
            if(server_port!=this.ssIP)send_query_to_server(server_port, lamp_clock, query);
        }
    }

    class ServerListener extends Thread {
        public void run(){
            while(true){
                try{
                    ServerSocket serverSocket=new ServerSocket(12346);
                    while(true){
                        Socket socket=serverSocket.accept();
                        DataInputStream dataInputStream=new DataInputStream(socket.getInputStream());
                        String ack_query=dataInputStream.readUTF();
                        String[] ss=ack_query.split("$");
                        Integer lck=new Integer(Integer.parseInt(ss[0]));
                        String query=ss[1];
                        if(lck>lamport_clock){lamport_clock=(lck/10)*10+node_id;}
                        if(awaiting_approval.containsKey(lck)){
                            Integer ack=acks.get(lck);
                            ack++;
                            acks.remove(lck);
                            acks.put(lck,ack);
                            if(ack==number_of_servers){
                                System.out.println(String.format("Node %d has query %s", node_id, query));
                            }
                        }else{
                            //send multicast
                        }
                        // process ack_query and to get lamport clock and string
                        // update string and int
                        // if int is hit to 3 start processing it
                    }
                }catch(Exception ex){
                    System.out.print("Server Listener thread could not be started");
                }
            }
        }
    }

    class ClientListener extends Thread {

        public void run(){
            try{
                ServerSocket serverSocket=new ServerSocket(Constants.getServerClientPort(node_id));
                while(true){
                    Socket socket=serverSocket.accept();
                    DataInputStream  in =new DataInputStream(socket.getInputStream());
                    DataOutputStream out=new DataOutputStream(socket.getOutputStream());
                    String query=in.readUTF();
                    System.out.println("Server received:"+query);
                    try{
                        multiCastWriteQuery(query);
                        //Build a mechanism to recognize a READ/WRITE
//                        for (Row row : session.execute(query)) {
//                            out.writeUTF(row.toString());
//                        }
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
        this.lamport_clock=node_id;
        this.number_of_servers=Constants.PORT_LIST.length;
        this.ssIP=Constants.SERVER_SERVER[node_id-1];
        String dbIP = Constants.getDBIP(node_id);
        String keyspace = Constants.getKeySpace();
        cluster = Cluster.builder()
                .addContactPoints(dbIP)
                .build();
        session = cluster.connect(keyspace);
        new ClientListener().run();
        new ServerListener().run();

    }



    public static void main(String args[]) {
        if(args.length!=1){
            System.out.print("run as Server");
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