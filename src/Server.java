import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int port = 5005;
    public static final int numThread = 3;
    public static final int COMMAND_LIMIT = 4;  //Số token mà lệnh có: {Action}_chat_{name}_result
    public static final int COMMAND_NAME = 2;   //Vị trí token {name} là 2 (0_1_2_3)
    public static final int COMMAND_RESULT = 3; //Vị trí token {name} là 3 (0_1_2_3)
    //Lệnh giữa Client - Server
    public static final String KEY_WHEN_PAIR_LEFT = "left_chat";
    public static final String KEY_WHEN_YOU_LEFT = "end_chat";
    public static final String KEY_WHEN_CLOSE = "close_chat";
    public static final String KEY_WHEN_EXIT = "exit_chat";
    private static ServerSocket server = null;

    //Kiểu executor là Fixed
    public static ExecutorService executor = Executors.newFixedThreadPool(numThread);
    public static Vector<Worker> workers = new Vector<>();
    public static Vector<Worker> queue = new Vector<>();

    public static void main(String[] args) throws IOException {
        try {
            server = new ServerSocket(port);
            System.out.println("Server binding at port " + port);
            System.out.println("Waiting for client...");
            while(true) {
                Socket socket = server.accept();
                System.out.println("Client " + socket + " accepted.");
                Worker client = new Worker(socket);
                workers.add(client);
                executor.execute(client);

                //Nếu số lượng worker vượt quá số lượng đề ra thì báo quá tải
                if (workers.size() > numThread)
                    client.sendMessage("busy");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(server != null)
                server.close();
        }
    }
}