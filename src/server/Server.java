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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Server {
    Session session;
    Cluster cluster;

    int ssIP;
    //IP for Server-Server Communication
    int node_id;
    int lamport_clock;
    int number_of_servers;
    Lock lock=new ReentrantLock();
    Proposal current_proposal;
    Map<Integer,Proposal> proposal_ack_map=new HashMap<Integer, Proposal>();
    ArrayList<Proposal> pending_proposal_list=new ArrayList<Proposal>();
    Log log;

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
        log.log("Perform DB operation for Proposal:" + proposal.lamport_clock + " Query:" + proposal.query);
    }

    public void multicastAck(Proposal proposal){
        log.log("Multicasting Acknowledgement for Proposal"+proposal.lamport_clock);
        proposal.acked=true;
        proposal.no_of_acks=proposal.no_of_acks+1;
        sendStringToOtherServers(proposal.getAckMsgFromProposal());
    }

    class ProposalAccepterThread extends Thread{
        Proposal proposal;
        ProposalAccepterThread(Proposal proposal){
            this.proposal=proposal;
        }
        public void run(){
            log.log("Accepting Proposal"+proposal.lamport_clock);
            proposal_ack_map.put(new Integer(proposal.lamport_clock),proposal);
            current_proposal=proposal;
            proposal.acked=true;
            proposal.no_of_acks=proposal.no_of_acks+1;
            try {
                TimeUnit.SECONDS.sleep(20);
            }catch (InterruptedException ex){

            }
            log.log("Sending Accepted proposal to others");
            sendStringToOtherServers(proposal.getSendMsgFromProposal());

        }
    }

    int acceptWriteQuery(String query){
        lock.lock();
        lamport_clock=lamport_clock+10;
        Proposal proposal=new Proposal(lamport_clock,query);
        lock.unlock();
        if(current_proposal==null){
            log.log("Accepting as no pending proposal");
            new ProposalAccepterThread(proposal).start();

        }else{
            log.log("Postponing");
            pending_proposal_list.add(proposal);
            //Other queries receive headway
        }
        return proposal.lamport_clock;

    }

    class ServerListener extends Thread {

        public void run(){
                try{
                    ServerSocket serverSocket=new ServerSocket(ssIP);

                    while(true){
                        Socket socket=serverSocket.accept();
                        DataInputStream dataInputStream=new DataInputStream(socket.getInputStream());
                        // Parsing string query
                        String ack_query=dataInputStream.readUTF();
                        log.log("Received on server port"+ack_query);
                        String[] ss=ack_query.split("\\$");
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
                                    new ProposalAccepterThread(pending_proposal_list.remove(0)).start();
                                }
                            }
                        }
                    }
                }catch(Exception ex){
                    log.log("Server Listener thread could not be started");

            }

        }
    }

    class ClientListener extends Thread {

        public void run(){
            try{
                int port=Constants.getServerClientPort(node_id);
                ServerSocket serverSocket=new ServerSocket(Constants.getServerClientPort(node_id));
                while(true){
                    Socket socket=serverSocket.accept();
                    DataInputStream  in =new DataInputStream(socket.getInputStream());
                    DataOutputStream out=new DataOutputStream(socket.getOutputStream());
                    int query_clock=in.readInt();
                    System.out.println("Last seen write's Lamport Clock"+query_clock);

                    String query=in.readUTF();
                    log.log("Server received:" + query);
                    try{
                        if(query_clock>lamport_clock){
                            log.log("Database is stale");
                            out.writeInt(query_clock);
                        }else{
                            if(query.toLowerCase().contains("select")){
                                //read Query
                                out.writeInt(lamport_clock);
                                for (Row row : session.execute(query)) {
                                    out.writeUTF(row.toString());
                                }
                            }else{
                                out.writeInt(acceptWriteQuery(query));
                            }
                        }

                    }catch (SyntaxError se){
                        out.writeUTF("Syntax Error.Try again");
                    }
                    socket.close();
                }
            }catch(Exception exception){
               log.log("Unable start Client listener socket");
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
//        cluster = Cluster.builder()
//                .addContactPoints(dbIP)
//                .build();
//        session = cluster.connect(keyspace);
        log=new Log(node_id);

        new ServerListener().start();
        new ClientListener().start();

    }



    public static void main(String args[]) {

        if(args.length!=1){
            System.out.print("run as Server");
        }
        int node_id=Integer.parseInt(args[0]);
        new Server(node_id);
        }
    }