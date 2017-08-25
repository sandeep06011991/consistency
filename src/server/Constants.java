package server;

/**
 * Created by poli on 24/8/17.
 */
public class Constants {
    //Use only odd ports
    //used for client-server communication
    static int[] PORT_LIST={12345,12347};
    static int[] SERVER_SERVER={12346,12348};
    static String[] DB_IP={"127.0.0.1","127.0.0.2"};
    static String KEYSPACE="repl1";
    public static int getServerClientPort(int node_id){
        return PORT_LIST[node_id-1];
    }

    public static String getDBIP(int node_id){
        return DB_IP[node_id-1];
    }
    
    public static int getServerServerPort(int node_id){
        return getServerClientPort(node_id)+1;
    }
    public static String getKeySpace(){return KEYSPACE;}
}
