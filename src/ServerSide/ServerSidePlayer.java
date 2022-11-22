package ServerSide;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerSidePlayer extends Thread{
    int port = 12345;
    Socket socketToClient;
    ObjectOutputStream oos;
    Protocol protocol = new Protocol();
    public ServerSidePlayer(Socket socketToClient){
        this.socketToClient = socketToClient;
    }

    public void protocolNextStage() throws IOException {
        this.oos.writeObject(protocol.processStage(null));
    }

    public void run(){
        try     (
                ObjectOutputStream oos = new ObjectOutputStream(socketToClient.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socketToClient.getInputStream());
                )
        {
            this.oos = oos;
            Object fromUser;
            Object fromServer;

            oos.writeObject(protocol.processStage(null));

            while((fromUser = ois.readObject()) != null){
                oos.writeObject(protocol.processStage(fromUser));
                // FÖR DEBUGGING:
                System.out.println("Sent: " + fromUser);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

}
