import java.io.BufferedReader;
import java.io.IOException;

class ClientReceiver implements Runnable {
    private final BufferedReader in;

    public ClientReceiver(BufferedReader in) {
        this.in = in;
    }

    public void run() {
        try {
            while(true) {
                //Lắng nghe message từ Server
                String data = in.readLine();

                //Nếu thông báo bạn chat đã rời phòng
                if (data.equals(Client.KEY_WHEN_PAIR_LEFT)) {
                    Client.sendMessage(Client.KEY_WHEN_CLOSE);
                    Client.alert(Client.getPairName() + " left the chat.");
                    break;
                }
                //Nếu nhận lệnh người dùng dừng chat
                if (data.equals(Client.KEY_WHEN_YOU_LEFT))
                    break;

                //decode text một dòng thành nhiều dòng bằng cách đổi %n% thành \n
                data = data.replaceAll("%n%","\n");
                Client.chat.appendChat(Client.getPairName().toUpperCase() + ": " + data + "\n");
            }

            //đóng chat thì vòng lặp while bị break do lệnh dừng chat.
            System.out.println("Chat closed.");
            Client.closeChat();
        } catch (IOException | NullPointerException ignored) {
            //Báo lỗi và đóng kết nối
            Client.alert("Server closed !");
            try {
                Client.close();
                System.out.println("Chat closed.");
                Client.closeChat();
            } catch (IOException ignored1) {}
            Client.clearConnect();
        }
    }
}