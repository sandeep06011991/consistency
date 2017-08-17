package  server;


import java.io.IOException;
import java.util.logging.*;


class Log{
    public static Logger logger=Logger.getLogger("server");


    private Log(){}

    private static boolean initalized=false ;

    private static void initialize(){
        try {
            logger.setLevel(Level.FINE);
            Handler handler = new FileHandler("server.log");
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        }catch(IOException ex){
            System.out.println("Could not create a log file");
        }
        initalized=true;
    }

    public static void info(String msg){
        if(!initalized)initialize();
        logger.info(msg);
    }
}