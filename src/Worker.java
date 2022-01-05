import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.*;
import java.util.regex.Pattern;

public class Worker implements Runnable {
    private String myName;
    private Worker pair;    //worker của người ghép cặp
    private final Socket socket;
    public BufferedReader in;
    public BufferedWriter out;
    private boolean isPaired = false;

    //Danh sách worker đã từ chối ghép cặp
    public Vector<Worker> denied = new Vector<>();
    //Danh sách lịch sử chat với mỗi element là danh sách chat với worker nào đó
    public HashMap<Worker, ArrayList<String>> histories = new HashMap<>();

    public Worker(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public String getName() {
        return myName;
    }

    public void setName(String myName) {
        this.myName = myName;
    }

    //Set đối tượng ghép cặp
    public void pairWith(Worker pair) {
        this.pair = pair;
    }

    //Khóa đối tượng muốn bắt cặp lại tránh xung đốt
    public void lockPair() {
        if (!isPaired)
            isPaired = true;
    }

    //Mở khóa đối tượng
    public void unlockPair() {
        if (isPaired)
            isPaired = false;
    }

    //Kiểm tra đối tượng có bị khóa không
    public boolean isPaired() {
        return isPaired;
    }

    //Loại bỏ ghép cặp khi có 1 bên thoát chat
    public void breakPair() throws IOException {
        pair.pairWith(null);
        pair.unlockPair();
        pair.sendMessage(Server.KEY_WHEN_PAIR_LEFT);
        pairWith(null);
        unlockPair();
        sendMessage(Server.KEY_WHEN_YOU_LEFT);
    }

    //Thực hiện ghép cặp khi cả 2 bên đồng ý
    public void doPair(Worker withWorker) throws IOException {
        System.out.println("Pair " + myName + " with " + withWorker.getName());
        Server.queue.remove(withWorker);
        lockPair();
        pairWith(withWorker);
        withWorker.lockPair();
        withWorker.pairWith(Worker.this);
        sendMessage("paired_chat_" + withWorker.getName());
        withWorker.sendMessage("paired_chat_" + myName);
    }

    public void sendMessage(String message) throws IOException {
        out.write(message);
        out.newLine();
        out.flush();
    }

    public String receiveMessage() throws IOException {
        return in.readLine();
    }

    public void run() {
        try {
            String getName = receiveMessage();
            boolean isNamed = checkName(getName);
            if (isNamed) {
                try {
                    findPair();
                } catch (Exception e) {
                    isNamed = false;
                }
            }

            while(isNamed) {
                String received = receiveMessage();

                if (received.startsWith("accept_chat_")) {
                    String[] split = received.split("_", Server.COMMAND_LIMIT);
                    if (split.length == Server.COMMAND_LIMIT) {
                        String name = split[Server.COMMAND_NAME];
                        for (Worker worker: Server.workers) {
                            if (name.equalsIgnoreCase(worker.myName)) {
                                worker.sendMessage(received);
                            }
                        }
                    }
                    continue;
                }

                if (received.equals(Server.KEY_WHEN_CLOSE)) {
                    if (pair != null)
                        breakPair();
                        try {
                            findPair();
                        } catch (Exception e) {
                            break;
                        }
                    continue;
                }

                if (received.equals(Server.KEY_WHEN_EXIT))
                    break;

                if (pair == null)
                    continue;
                pair.sendMessage(received);
                try {
                    histories.get(pair).add(myName.toUpperCase() + ": " + received);
                    pair.histories.get(Worker.this).add(myName.toUpperCase() + ": " + received);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Closed socket for client " + myName + " : " + socket.toString());
            Server.queue.remove(Worker.this);
            Server.workers.remove(Worker.this);
            in.close();
            out.close();
            socket.close();
        } catch (IOException | NullPointerException ignored) {}
    }

    //Hàm kiểm tra tên có đúng quy định và tồn tại chưa
    public boolean checkName(String name) throws IOException {
        //Kiểm tra tên có ký tự đặc biệt không - nếu có isInvalid = TRUE
        boolean isInvalid = Pattern.compile("[!@#$%&*()_+=|<>?{}\\\\~-]", Pattern.CASE_INSENSITIVE)
                .matcher(name)
                .find();
        if (isInvalid)
            return false;
        //Gửi lệnh "name_used" cho Client nếu tên đã tồn tại hoặc sai quy định
        for (Worker worker : Server.workers) {
            if (name.equalsIgnoreCase(worker.getName())) {
                sendMessage("name_used");
                return false;
            }
        }
        setName(name);
        return true;
    }

    //Hàm thực hiện ghép cặp mỗi khi đăng nhập thành cộng (quan trọng !)
    public void findPair() throws Exception {
        while (true) {
            //Lấy ngẫu nhiên 1 worker trong hàng đợi
            Worker randomClient = getRandomFromQueue();

            //Nếu không có ai đợi thì hủy tìm kiếm và đưa người dùng này vào hàng chờ
            if (randomClient == null) {
                Server.queue.add(Worker.this);
                break;
            }

            //Gửi lệnh "invite_chat_{pairName}" hỏi người dùng có muốn ghép không
            sendMessage("invite_chat_" + randomClient.getName());
            String[] inviteReply = receiveMessage().split("_", Server.COMMAND_LIMIT);
            //Nếu True tức đồng ý ghép
            if (inviteReply[Server.COMMAND_RESULT].equals("true")) {
                //Nếu người đó đã ghép với người khác thì báo lại "invite_chat_{pairName}_fail"
                if (randomClient.isPaired()) {
                    sendMessage("invite_chat_" + randomClient.getName() + "_fail");
                    continue;
                }

                //Còn được thì gửi lệnh "accept_chat_myName" đến cho người muốn ghép để hỏi có đồng ý ghép với mình không.
                randomClient.sendMessage("accept_chat_" + myName);
                String[] acceptReply = receiveMessage().split("_", Server.COMMAND_LIMIT);
                //Nếu True tức đồng ý ghép
                if (acceptReply[Server.COMMAND_RESULT].equals("true")) {
                    //Thực hiện ghép cặp
                    doPair(randomClient);
                    loadHistoryChat(randomClient);
                    break;
                }
                else {
                    //người muốn ghép không đồng ý thì mở khóa và thêm họ vào danh sách từ chối.
                    randomClient.unlockPair();
                    denied.add(randomClient);
                }
            }
            //Nếu người dùng không muốn ghép với người server đề xuất thì thêm người đó vào danh sách từ chối.
            else denied.add(randomClient);
        }
    }

    //Lấy ngẫu nhiên từ danh sách chờ
    public Worker getRandomFromQueue() {
        Worker randomClient = null;
        Vector<Worker> temp = new Vector<>(Server.queue);
        if (temp.isEmpty())
            return null;

        temp.removeAll(denied);
        if (!temp.isEmpty()) {
            Random rand = new Random();
            randomClient = temp.remove(rand.nextInt(temp.size()));
            if (randomClient.isPaired())
                return getRandomFromQueue();
        }
        return randomClient;
    }

    //Lấy lịch sử chat giữa 2 người dùng từ lịch sử của người thực hiện ghép cặp.
    public void loadHistoryChat(Worker chatWith) throws IOException {
        if (!histories.containsKey(chatWith))
            histories.put(chatWith, new ArrayList<>());
        if (!chatWith.histories.containsKey(Worker.this))
            chatWith.histories.put(Worker.this, new ArrayList<>());

        StringBuilder builder = new StringBuilder();
        for (String history: histories.get(chatWith)) {
            builder.append(history).append("%n%");
        }
        sendMessage(builder.toString());
        chatWith.sendMessage(builder.toString());
    }
}