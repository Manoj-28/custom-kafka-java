import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Main {
    private static final int UNSUPPORTED_VERSION_ERROR_CODE = 35;
    private static final int NO_ERROR_CODE = 0;
    private static final int API_VERSIONS_KEY = 18;
    private static final int PORT = 9092;

    private static void sendErrorResponse(OutputStream out, byte[] correlationIdBytes) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(correlationIdBytes);       // Correlation ID
        bos.write(new byte[] {0, 35});       // Error code
        int size = bos.size();
        byte[] sizeBytes = ByteBuffer.allocate(4).putInt(size).array();
        System.out.println(Arrays.toString(sizeBytes));
        out.write(sizeBytes);       //message size
        out.write(bos.toByteArray());
        System.err.println("Sent Error Response with Code: " + UNSUPPORTED_VERSION_ERROR_CODE);
    }

    private static void sendAPIVersionsResponse(OutputStream out, byte[] correlationIdBytes) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(correlationIdBytes);       // Correlation ID
        bos.write(new byte[] {0, 0});        // No error
        bos.write(2);
        bos.write(new byte[] {0, 18});       // API key
        bos.write(new byte[] {0, 0});        // Min version
        bos.write(new byte[] {0, 4});        // Max version
        bos.write(0);                        // Tagged fields
        bos.write(new byte[] {0, 0, 0, 0});  // Throttle time
        bos.write(0);          // End of Tagged fields
        int size = bos.size();
        byte[] sizeBytes = ByteBuffer.allocate(4).putInt(size).array();
        System.out.println(Arrays.toString(sizeBytes));
        System.out.println(Arrays.toString(bos.toByteArray()));
        out.write(sizeBytes);

        out.write(bos.toByteArray());
        out.flush();
        System.err.println("Sent APIVersions response with no error.");
    }

    public static void main(String[] args) {
        System.err.println("Logs from your program will appear here!");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            try (Socket clientSocket = serverSocket.accept()) {
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();

                in.readNBytes(4);  // Skip message size

                byte[] apiKeyAndVersion = in.readNBytes(4);
                int apiVersion = ((apiKeyAndVersion[2]) << 8) | (apiKeyAndVersion[3]);
                System.err.println("API version: " + apiVersion);

                byte[] correlationIdBytes = in.readNBytes(4);

                if (apiVersion < 0 || apiVersion > 4) {
                    sendErrorResponse(out, correlationIdBytes);
                } else {
                    sendAPIVersionsResponse(out, correlationIdBytes);
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
