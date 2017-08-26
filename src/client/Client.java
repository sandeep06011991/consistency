package client;

import java.io.Console;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.DataOutputStream;

public class Client{
    public static void process(String hostname,String query,int port){
        try {
            Socket client = new Socket(hostname,port);
            DataOutputStream out=new DataOutputStream(client.getOutputStream());
            DataInputStream in =new DataInputStream(client.getInputStream()) ;
            out.writeUTF(query);
            while(true) {
                String str = in.readUTF();
                System.out.println(str);
            }
        }catch (IOException e){
            e.printStackTrace();
            System.out.println("Finished reading");
        }
    }

    public static void main(String args[]){
        Console console=System.console();
        String hostname;
        int port;
        String query;
        System.out.println("Enter string in the following format");
        System.out.println("[Port]:[IP] \"<Query to execute\"");
        while(true){
            String s=console.readLine();
            String[] ss=s.split("\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?");
            if(ss.length!=2){System.out.println("Incorrect format");continue;}
            String[] s2=ss[0].split(":");
            port=Integer.parseInt(s2[0]);
            hostname=s2[1];
            query=ss[1];
            process(hostname,query,port);
        }

    }
}