import javax.swing.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private static final String host = "localhost";
    private static final int port = 5005;
    public static final int COMMAND_LIMIT = 4;  //Số token mà lệnh có: {Action}_chat_{name}_result
    public static final int COMMAND_NAME = 2;   //Vị trí token {name} là 2 (0_1_2_3)
    public static final int COMMAND_RESULT = 3; //Vị trí token {name} là 3 (0_1_2_3)
    //Lệnh giữa Client - Server
    public static final String KEY_WHEN_PAIR_LEFT = "left_chat";
    public static final String KEY_WHEN_YOU_LEFT = "end_chat";
    public static final String KEY_WHEN_CLOSE = "close_chat";
    public static final String KEY_WHEN_EXIT = "exit_chat";

    private static Socket socket;
    private static BufferedWriter out;
    private static BufferedReader in;

    public static ChatFrame chat;
    public static RegisterForm form;
    private static String name;         //name là tên người dùng
    private static String pairName;     //pairName là người ghép cặp để chat
    //Kiểu executor là Single
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void setName(String name) throws IOException {
        Client.name = name;
        sendMessage(name);
    }

    public static String getName() {
        return name;
    }

    public static String getPairName() {
        return pairName;
    }

    //Khởi tạo kết nối đến Server
    public static void connect() throws IOException {
        socket = new Socket(host, port);
        System.out.println("Client connected.");
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static boolean isConnected() {
        return socket != null;
    }

    public static void clearConnect() {
        socket = null;
        form.isWait(false);
    }

    //Khởi tạo giao diện chat sau khi nhận lệnh "paired_{name}"
    public static void startChat() throws IOException {
        String historyChat = receive().replaceAll("%n%","\n");
        chat = new ChatFrame();
        chat.toFront();
        chat.appendChat(historyChat);
        form.setVisible(false);
        executor.execute(new ClientReceiver(in));
    }

    //Đóng chat
    public static void closeChat() throws IOException {
        pairName = null;
        chat.dispose();
        form.setVisible(true);
        form.toFront();
        findChat();
    }

    public static void sendMessage(String message) throws IOException {
        out.write(message);
        out.newLine();
        out.flush();
    }

    public static String receive() throws IOException {
        return in.readLine();
    }

    public static void close() throws IOException {
        out.close();
        in.close();
        socket.close();
    }

    //Thông báo
    public static void alert(String message) {
        form.toFront();
        JOptionPane.showMessageDialog(form, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    //Hiện giao diện hỏi Yes_NO
    public static int confirm(String title, String message) {
       return JOptionPane.showConfirmDialog(form, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    //Tìm kiếm bạn chat(hàm chức năng chính !)
    public static void findChat() throws IOException, InputMismatchException {
        while (true) {
            String response = receive();
            //xử lý theo lệnh nhận được từ phía Server theo IF

            if (response == null)
                continue;

            if (response.equals("name_used")) {
                alert(name + " already in use !");
                setName(null);
                throw new InputMismatchException();
            }

            if (response.equals("busy")) {
                alert("Server Overload !");
                continue;
            }

            if (response.startsWith("invite_chat_")) {
                replyInviteRequest(response);
                continue;
            }

            if (response.startsWith("accept_chat_")) {
                replyAcceptRequest(response);
                continue;
            }

            if (response.startsWith("paired_chat_")) {
                doPairAndStartChat(response);
                break;
            }
        }
    }

    //Xử lý lệnh "invite_chat_{name}"
    public static void replyInviteRequest(String command) throws IOException {
        //Bóc tách câu lệnh với split
        String[] split = command.split("_", COMMAND_LIMIT);
        String pairName = split[COMMAND_NAME];
        //Trường hợp "invite_chat_{name}_fail" tức người muốn bắt cặp đã bắt cặp với người khác.
        if (split.length == COMMAND_LIMIT) {
            String status = split[COMMAND_RESULT];
            if (status.equals("fail"))  {
                alert(pairName +" chatted with someone else !");
            }
            return;
        }
        form.toFront();
        //Thông báo
        int choice = confirm("Invite chat","Invite " + pairName + " to chat with you ?");
        if (choice == JOptionPane.YES_OPTION)
            sendMessage(command + "_true");     //yes
        else sendMessage(command + "_false");   //no
    }

    //Xử lý lệnh "accept_chat_{name}"
    public static void replyAcceptRequest(String command) throws IOException {
        //Bóc tách câu lệnh với split
        String[] split = command.split("_", COMMAND_LIMIT);
        if (split.length == COMMAND_LIMIT) {
            if (split[COMMAND_RESULT].equals("false")) {
                alert("Your invitation declined !");
            }
            sendMessage(command);
            return;
        }
        String pairName = split[COMMAND_NAME];
        form.toFront();
        //Thông báo
        int choice = confirm("Accept chat", "Accept " + pairName + "'s invitation?");
        if (choice == JOptionPane.YES_OPTION)
            sendMessage(command + "_true");     //yes
        else sendMessage(command + "_false");   //no
    }

    //Xử lý lệnh "paired_chat_{name}"
    public static void doPairAndStartChat(String command) throws IOException {
        //lấy tên người gắp cặp {name} từ lệnh
        String[] split = command.split("_", COMMAND_LIMIT);
        pairName = split[COMMAND_NAME];
        System.out.println("Start chat with " + pairName);
        startChat();
    }

    public static void main(String[] args) {
        form = new RegisterForm();
    }
}