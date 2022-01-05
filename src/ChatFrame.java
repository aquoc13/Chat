import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ChatFrame extends JFrame {
    private JPanel mainPanel;
    private JTextArea areaChat;
    private JTextArea areaInput;
    private JButton btnSend;
    private JPanel jpanel;
    private JScrollPane jScrollChat;
    private JScrollPane jScrollInput;

    public ChatFrame() {
        initComponent();

        setContentPane(mainPanel);
        setTitle("Chat with " + Client.getPairName());
        setSize(500,400);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(Client.form);
        setVisible(true);

        //Event khi đóng cửa window
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    //Gửi lệnh dừng chat.
                    Client.sendMessage(Client.KEY_WHEN_CLOSE);
                } catch (IOException ignored) {}
            }
        });
    }

    //Khởi tạo Component cho GUI
    public void initComponent() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        jpanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        areaChat.setEditable(false);
        areaChat.setFocusable(false);
        areaChat.setColumns(40);
        areaChat.setRows(15);
        areaChat.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        areaChat.setLineWrap(true);
        DefaultCaret caret = (DefaultCaret) areaChat.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        areaInput.setColumns(34);
        areaInput.setRows(3);
        areaInput.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        areaInput.setLineWrap(true);

        btnSend.setFocusPainted(false);
        btnSend.setBorder(BorderFactory.createEmptyBorder(22, 16, 22, 16));

        //Event khi bấm nút SEND
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //getText rồi dùng trim() bỏ khoảng trắng hoặc xuống dòng ở 2 đầu text
                    String input = areaInput.getText().trim();
                    if (input.isEmpty())
                        return;
                    areaChat.append(Client.getName().toUpperCase() + ": " + input + "\n");
                    //encode text nhiều dòng thành 1 dòng bằng cách đổi \n thành %n%
                    input = input.replaceAll("\n","%n%");
                    //Gửi message đến Server
                    Client.sendMessage(input);
                    areaInput.setText("");
                } catch (IOException ex) {
                    //Lỡi không thể send message thì đóng kết nối + chat rồi mở lại form đăng nhập
                    try {
                        Client.sendMessage(Client.KEY_WHEN_CLOSE);
                    } catch (IOException ignored) {}
                    dispose();
                    Client.form.setVisible(true);
                    Client.form.toFront();
                }
            }
        });
    }

    //Thêm text vào Chat Box
    public void appendChat(String text) {
        areaChat.append(text);
    }
}
