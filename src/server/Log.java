package  server;


import java.io.IOException;
import java.util.logging.*;


class Log{

    Logger logger;
    String name;
    Log(int node_id){
        logger=Logger.getLogger("server");
        try {
            name="Server"+node_id;
            logger.setLevel(Level.FINE);
            Handler handler = new FileHandler("log/server"+node_id+".log");
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        }catch(IOException ex){
            System.out.println("Could not create a log file");
        }
    }

    void log(String msg){
        logger.info(name+": "+msg);
    }
}