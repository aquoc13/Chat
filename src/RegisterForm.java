import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.regex.Pattern;

public class RegisterForm extends JFrame{
    private JPanel mainPanel;
    private JTextField txtName;
    private JLabel lbName;
    public JButton btnJoin;

    private static boolean isConnected = false;

    public RegisterForm() {
        initComponent();

        setContentPane(mainPanel);
        setTitle("Welcome");
        setSize(250,130);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        //Event khi đóng cửa window
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isConnected) {
                    try {
                        //Gửi lệnh ngắt kết nối
                        Client.sendMessage(Client.KEY_WHEN_EXIT);
                        Client.close();
                    } catch (IOException ignored) {}
                }
            }
        });
    }

    //Khởi tạo Component cho GUI
    public void initComponent() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        btnJoin.setFocusPainted(false);
        btnJoin.setCursor(new Cursor(java.awt.Cursor.HAND_CURSOR));

        //Event khi bấm nút JOIN (kết nối) hoặc CANCEL (Ngắt kết nối)
        btnJoin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Kiểm tra tên có ký tự đặc biệt không - nếu có isInvalid = TRUE
                boolean isInvalid = Pattern.compile("[!@#$%&*()_+=|<>?{}\\\\~-]", Pattern.CASE_INSENSITIVE)
                        .matcher(txtName.getText())
                        .find();

                if (isInvalid || txtName.getText().isEmpty() || txtName.getText().isBlank()) {
                    //Thông báo
                    Client.alert("Invalid name (contains a special character).");
                    return;
                }

                //Nếu chưa connect (isConnected=false) thì thực hiện kết nối và ngược lại.
                if (!isConnected) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String name = txtName.getText();
                            try {
                                if (!Client.isConnected()) {
                                    Client.connect();
                                }
                                isWait(true);
                                //Đặt username đã nhập cho Client
                                Client.setName(name);
                                //Tìm kiếm bạn chat
                                Client.findChat();
                            } catch (IOException | NullPointerException e) {
                                //Báo lỗi
                                Client.alert("Closed connect with server !");
                                isWait(false);
                                Client.clearConnect();
                            } catch (InputMismatchException ignore) {
                                isWait(false);
                                Client.clearConnect();
                            }
                        }
                    }).start();
                }
                else {
                    try {
                        //GỬi lệnh ngắt kết nối tới Server
                        Client.sendMessage(Client.KEY_WHEN_EXIT);
                        isWait(false);
                        Client.close();
                    } catch (IOException | NullPointerException ignored) {}
                    try {
                        Client.close();
                    } catch (IOException ignored) {}
                }
            }
        });
    }

    //Điều chỉnh GUI theo trang thái
    public void isWait(boolean status) {
        //TRUE - đợi kết nối
        if (status) {
            btnJoin.setText("Cancel");
            setTitle("Wait...");
            txtName.setEnabled(false);
            isConnected = true;
        }
        else {
            btnJoin.setText("Join");
            setTitle("Register");
            txtName.setEnabled(true);
            isConnected = false;
        }
    }
}
