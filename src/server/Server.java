package  server;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.SyntaxError;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;


public class Server {
    Session session;
    Cluster cluster;

    int ssIP;
    //IP for Server-Server Communication
    int node_id;
    int lamport_clock;
    int number_of_servers;
    Lock lock;
    Proposal current_proposal;
    Map<Integer,Proposal> proposal_ack_map=new HashMap<>();
    ArrayList<Proposal> pending_proposal_list=new ArrayList<>();


    void sendStringToOtherServers(String msg){
        for(int server_port:Constants.SERVER_SERVER){
            // Only do this for servers other than me
            if(server_port!=this.ssIP){
                try {
                    Socket client = new Socket("127.0.0.1",server_port);
                    DataOutputStream out=new DataOutputStream(client.getOutputStream());
                    out.writeUTF(msg);
                    client.close();
                }catch (IOException e){
                    System.out.println("Finished Sending update query reading");
                }
            }
        }
    }

    void performWriteQuery(Proposal proposal){
        proposal_ack_map.remove(proposal.lamport_clock);
        System.out.println("Proposal:"+proposal.lamport_clock+" Query:"+proposal.query);
    }

    public void multicastAck(Proposal proposal){
        proposal.acked=true;
        proposal.no_of_acks=proposal.no_of_acks+1;
        sendStringToOtherServers(proposal.getAckMsgFromProposal());
    }

    void acceptProposal(Proposal proposal){
        proposal_ack_map.put(new Integer(proposal.lamport_clock),proposal);
        current_proposal=proposal;
        proposal.acked=true;
        proposal.no_of_acks=proposal.no_of_acks+1;
        sendStringToOtherServers(proposal.getSendMsgFromProposal());
    }

    void acceptWriteQuery(String query){
        lock.lock();
        lamport_clock=lamport_clock+10;
        Proposal proposal=new Proposal(lamport_clock,query);
        lock.unlock();
        if(current_proposal==null){
            acceptProposal(proposal);
        }else{
            pending_proposal_list.add(proposal);
            //Other queries receive headway
        }

    }


    class ServerListener extends Thread {

        public void run(){
            while(true){
                try{
                    ServerSocket serverSocket=new ServerSocket(ssIP);
                    while(true){
                        Socket socket=serverSocket.accept();
                        DataInputStream dataInputStream=new DataInputStream(socket.getInputStream());
                        // Parsing string query
                        String ack_query=dataInputStream.readUTF();
                        String[] ss=ack_query.split("$");
                        String key=ss[0];
                        Integer lck=new Integer(Integer.parseInt(ss[1]));
                        String query=ss[2];
                        //End of Parse string

                        lock.lock();
                        if(lck>lamport_clock){lamport_clock=(lck/10)*10+node_id;}
                        lock.unlock();

                        if(!proposal_ack_map.containsKey(lck)){
                            proposal_ack_map.put(lck,new Proposal(lck.intValue(),query));
                        }
                        Proposal proposal=proposal_ack_map.get(lck);

                        if(key.equals("SEND")){
                            proposal.no_of_acks=proposal.no_of_acks+1;//Sender ACK
                            if((current_proposal!=null)&&(current_proposal.lamport_clock<proposal.lamport_clock)){
                                System.out.println("Blocking acknowledgement for "+proposal.lamport_clock);
                            }else{
                                multicastAck(proposal);
                            }
                        }else{
                            assert key.equals("ACK");
                            proposal.no_of_acks=proposal.no_of_acks+1;//Sender ACK
                        }

                        if(proposal.no_of_acks==number_of_servers){
                            performWriteQuery(proposal);
                            if(current_proposal==proposal){
                                current_proposal=null;
                                //The order of acking pending proposals or acceting new proposal does not matter
                                //Interchange and run tests
                                for(Proposal p:proposal_ack_map.values()){
                                    if(!p.acked){
                                        multicastAck(p);
                                        if(p.no_of_acks==number_of_servers){
                                            performWriteQuery(p);
                                        }
                                    }
                                }
                                if(!pending_proposal_list.isEmpty()){
                                    pending_proposal_list.remove(0);

                                }
                            }

                        }
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
                        acceptWriteQuery(query);
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
        //setup some constants
        String dbIP = Constants.getDBIP(node_id);
        String keyspace = Constants.getKeySpace();
        cluster = Cluster.builder()
                .addContactPoints(dbIP)
                .build();
        session = cluster.connect(keyspace);
        new ServerListener().run();
        new ClientListener().run();
    }



    public static void main(String args[]) {
        if(args.length!=1){
            System.out.print("run as Server");
        }
        int node_id=Integer.parseInt(args[0]);
        new Server(node_id);
        }
    }