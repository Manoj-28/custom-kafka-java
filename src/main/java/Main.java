import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args){
        System.err.println("Logs from your program will appear here!");

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 9092;
        try {
            serverSocket = new ServerSocket(port);
            // REUSEADDR ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            // Wait for connection from client.
            clientSocket = serverSocket.accept();

            //echo -n "00000023001200046f7fc66100096b61666b612d636c69000a6b61666b612d636c6904302e3100" | xxd -r -p | nc localhost 9092 | hexdump -C
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            byte[] messageSize = new byte[4];
            in.read(messageSize);

            byte[] apiKeyAndVersion = new byte[4];
            in.read(apiKeyAndVersion);

            int apiVersion = ((apiKeyAndVersion[2] & 0xFF) << 8 | (apiKeyAndVersion[3] & 0xFF));
            System.err.println("API version: " + apiVersion);

            byte[] correlationIdBytes = new byte[4];
            in.read(correlationIdBytes);

            out.write(new byte[] {0,0,0,0});   //Message size
            out.write(correlationIdBytes);   // Correlation ID

            boolean isValidVersion = apiVersion >= 0 && apiVersion <=4;

            if(!isValidVersion){

                //error code 35 for unsupported version
                out.write(new byte[] {0,35});
            }
            else{
                System.err.println("Received supported API version.");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
