package server;


public class Proposal {
    public final int lamport_clock;
    public final String query;
    public boolean acked=false;
    public int no_of_acks=0;
    Proposal(int lamport_clock,String query){
        this.lamport_clock=lamport_clock;
        this.query=query;
    }
    String getSendMsgFromProposal(){
        return "SEND$"+lamport_clock+"$"+query;
    }

    String getAckMsgFromProposal(){
        return "ACK$"+lamport_clock+"$"+query;
    }

}
