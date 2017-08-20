package client;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client{
    public static void main(String args[]){
        try {
            Socket client = new Socket("localhost", 12345);
            DataOutputStream out=new DataOutputStream(client.getOutputStream());
            out.writeUTF("Client says Hi");
            out.writeUTF("Client says Hi again");
            client.close();
        }catch (Exception e){
            System.out.println("socket unable to open");
        }
    }
}