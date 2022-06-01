package FTPClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;

public class MainFrame extends JFrame {

    private FTPManager ftpManager;

    private BorderLayout bl;
    private JPanel inputPanel;
    private JTextField hostField;
    private JTextField idField;
    private JPasswordField pwField;
    private JTextField portField;
    private JToggleButton connectBtn;
    private ButtonGroup buttonGroup = new ButtonGroup();

    private JList<DirectoryItem> serverDirectoryList;
    private JList<DirectoryItem> clientDirectoryList;

    private JTextPane msgField;
    private String msgs = "";
    private JPanel panel;
    private JPanel panel_1;
    private JLabel lblNewLabel;
    private JLabel lblNewLabel_1;
    private JToggleButton btnNgtKtNi;
    private JPanel panel_2;
    private JTextField pathclient;
    private JTextField pathserver;

    public MainFrame(){

        Dimension mainSize = new Dimension(1200, 750); 
        this.setSize(mainSize);


        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                ftpManager.quitServer();
            }
        });


        bl = new BorderLayout();
        getContentPane().setLayout(bl);

        { 
            inputPanel = new JPanel(); 
            inputPanel.setSize(mainSize.width, (int)Math.max(mainSize.getHeight()*0.05, 30)); 
            inputPanel.setLayout(new BorderLayout(0, 0));


            getContentPane().add(inputPanel, BorderLayout.NORTH);
                                            
                panel_2 = new JPanel();
                inputPanel.add(panel_2, BorderLayout.NORTH);
                
                    JLabel label = new JLabel("Host:");
                    panel_2.add(label);
                    hostField = new JTextField("localhost");
                    panel_2.add(hostField);
                    hostField.setPreferredSize(new Dimension(150, 37));
                    JLabel label_1 = new JLabel("User:");
                    panel_2.add(label_1);
                    idField = new JTextField();
                    panel_2.add(idField);
                    idField.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
                    JLabel label_2 = new JLabel("Password:");
                    panel_2.add(label_2);
                    pwField = new JPasswordField();
                    panel_2.add(pwField);
                    pwField.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
                    JLabel label_3 = new JLabel("Port:");
                    panel_2.add(label_3);
                    portField = new JTextField();
                    panel_2.add(portField);
                    portField.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
                    connectBtn = new JToggleButton("Kết nối");
                    panel_2.add(connectBtn);
                    connectBtn.setPreferredSize(new Dimension(100, inputPanel.getSize().height));
                    connectBtn.addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ftpManager.connectFTPServer(
                                    hostField.getText().toString(),
                                    idField.getText().toString(),
                                    new String(pwField.getPassword()),
                                    portField.getText().toString()
                                    );
                            JOptionPane.showMessageDialog(MainFrame.this, "Đã Kết nối tới server");
                        }
                    });
                    
                    buttonGroup.add(connectBtn);
                    
                    btnNgtKtNi = new JToggleButton("Ngắt kết nối");
                    panel_2.add(btnNgtKtNi);
                    btnNgtKtNi.setPreferredSize(new Dimension(130, 37));
                    btnNgtKtNi.addActionListener(new ActionListener() {
                    	public void actionPerformed(ActionEvent e) {
                    		ftpManager.quitServer();
                    		JOptionPane.showMessageDialog(MainFrame.this, "Đã ngắt kết nối");
                    	}
                    });
                    buttonGroup.add(btnNgtKtNi);
        
                {
        
                    JScrollPane scroll = new JScrollPane ();
                    inputPanel.add(scroll, BorderLayout.SOUTH);
                    scroll.setPreferredSize(new Dimension(mainSize.width, (int)Math.max(mainSize.height*0.25, 100)));
                    scroll.setViewportView(msgField);
                    msgField = new JTextPane();
                    scroll.setViewportView(msgField);
                    String initPath = new File(".").getAbsolutePath();
                    ftpManager = new FTPManager(serverDirectoryList, clientDirectoryList, initPath, msgField, pathclient, pathserver);
                }
        }

        {
            Dimension listSize = new Dimension(mainSize.width, (int)Math.max(mainSize.height*0.7, 200)); 
            JPanel directoryPanel = new JPanel();
            directoryPanel.setLayout(new BorderLayout());

            // server directory
            serverDirectoryList = new JList<DirectoryItem>(); 
            serverDirectoryList.setCellRenderer(new DirectoryCellRenderer()); 
            serverDirectoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
            serverDirectoryList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JList list = (JList)e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) { 

                        // Double-click detected
                        ftpManager.selectServerListItem(index);

                    }

                    if (e.getButton() == MouseEvent.BUTTON3) {
                        // Right-click
                        JPopupMenu popupMenu = new JPopupMenu();

                        JMenuItem renameItem = new JMenuItem("Đổi tên folder");
                        renameItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String name = JOptionPane.showInputDialog(MainFrame.this, "Nhập tên folder mới");
                                if(name == null || "".equals(name)){ 
                                    JOptionPane.showMessageDialog(MainFrame.this, "Đổi tên thất bại");
                                }else{
                                    ftpManager.rename(index, name); // Rename 
                                    JOptionPane.showMessageDialog(MainFrame.this, "Đổi tên thành công");
                                }
                            }
                        });
                        JMenuItem deleteItem = new JMenuItem("Xoá file");
                        deleteItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {

                            	int r = JOptionPane.showConfirmDialog(MainFrame.this, "Ban thật sự muốn xoá file ?", "Xoá file", JOptionPane.YES_NO_OPTION);
                                if (r == JOptionPane.YES_OPTION) {
                                	
                                	ftpManager.delete(index);
                                    JOptionPane.showMessageDialog(MainFrame.this, "Đã xoá file");    
                                }          
                            }
                        });
                        JMenuItem dowloadItem = new JMenuItem("Tải xuống");
                        dowloadItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                            	ftpManager.selectServerListItem(index);
                            	JOptionPane.showMessageDialog(MainFrame.this, "Đã tải xuống file");
                            }
                        });
                        JMenuItem makeDirectoryItem = new JMenuItem("Tạo thư mục");
                        makeDirectoryItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String name = JOptionPane.showInputDialog(MainFrame.this, "Nhập tên thư mục mới");
                                if(name == null || "".equals(name)){ 
                                    JOptionPane.showMessageDialog(MainFrame.this, "Tạo thư mục thất bại");
                                }else{
                                    ftpManager.makeServerDirectory(name);  
                                    JOptionPane.showMessageDialog(MainFrame.this, "Tạo thư mục thành công");
                                }
                            }
                        });

                        popupMenu.add(dowloadItem);
                        popupMenu.add(renameItem);
                        popupMenu.add(deleteItem);
                        popupMenu.add(makeDirectoryItem);

                        popupMenu.show((Component)e.getSource(), e.getX(), e.getY()); 

                        list.setSelectedIndex(index);
                    }
                }
            });
            
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setPreferredSize(new Dimension(550, 500));
            scrollPane.setViewportView(serverDirectoryList);

            directoryPanel.add(scrollPane, BorderLayout.EAST);
            
            panel = new JPanel();
            scrollPane.setColumnHeaderView(panel);
            
            lblNewLabel_1 = new JLabel("Server");
            lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 13));
            panel.add(lblNewLabel_1);
            
            pathserver = new JTextField();
            pathserver.setEditable(false);
            panel.add(pathserver);
            pathserver.setColumns(40);


            // client directory
            clientDirectoryList = new JList<DirectoryItem>();
            clientDirectoryList.setCellRenderer(new DirectoryCellRenderer());
            clientDirectoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            clientDirectoryList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JList list = (JList)e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        // Double-click detected
                        ftpManager.selectClientListItem(index);
                    }
                    
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        // Right-click
                        JPopupMenu popupMenu = new JPopupMenu();

                        JMenuItem renameItem = new JMenuItem("Đổi tên folder client");
                        renameItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String name = JOptionPane.showInputDialog(MainFrame.this, "Nhập tên folder mới");
                                if (name == null || "".equals(name)){ 
                                    JOptionPane.showMessageDialog(MainFrame.this, "Chưa nhập tên folder mới");
                                } else {
                                    boolean error = ftpManager.clientRename(index, name); 
                                    JOptionPane.showMessageDialog(MainFrame.this, "Đổi tên thành công");
                                    if (!error) {
                                        JOptionPane.showMessageDialog(MainFrame.this, "Đổi tên thất bại");
                                    }
                                }
                            }
                        });

                        JMenuItem deleteItem = new JMenuItem("Xoá");
                        deleteItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                            	int r = JOptionPane.showConfirmDialog(MainFrame.this, "Ban thật sự muốn xoá file ?", "Xoá file", JOptionPane.YES_NO_OPTION);
                                if (r == JOptionPane.YES_OPTION) {
                                	
                                	boolean error = ftpManager.clientDelete(index); 
                                    JOptionPane.showMessageDialog(MainFrame.this, "Đã xoá file");    
                                    if (!error) {
                                        JOptionPane.showMessageDialog(MainFrame.this, "Xoá file thất bại");
                                    }
                                }
                            }
                        });

                        JMenuItem uploadItem = new JMenuItem("Tải lên");
                        uploadItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                            	ftpManager.selectClientListItem(index);
                            	JOptionPane.showMessageDialog(MainFrame.this, "Đã tải lên file");
                            }
                        });
                        
                        JMenuItem makeDirectoryItem = new JMenuItem("Tạo thư mục");
                        makeDirectoryItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String name = JOptionPane.showInputDialog(MainFrame.this, "Nhập tên thư mục mới");
                                if (name == null || "".equals(name)){ 
                                    JOptionPane.showMessageDialog(MainFrame.this, "Chưa nhập tên thư mục");
                                } else {
                                    boolean error = ftpManager.makeClientDirectory(name); 
                                    JOptionPane.showMessageDialog(MainFrame.this, "Tạo thư mục thành công");
                                    if (!error) {
                                        JOptionPane.showMessageDialog(MainFrame.this, "Tạo thư mục thất bại");
                                    }
                                }
                            }
                        });
                        popupMenu.add(uploadItem);
                        popupMenu.add(renameItem);
                        popupMenu.add(deleteItem);
                        popupMenu.add(makeDirectoryItem);

                        popupMenu.show((Component)e.getSource(), e.getX(), e.getY());

                        list.setSelectedIndex(index);
                    }
                }
            });

            JScrollPane scrollPane2 = new JScrollPane();
            scrollPane2.setPreferredSize(new Dimension(550, 500));
            scrollPane2.setViewportView(clientDirectoryList);

            directoryPanel.add(scrollPane2, BorderLayout.WEST);
            
            panel_1 = new JPanel();
            scrollPane2.setColumnHeaderView(panel_1);
            
            lblNewLabel = new JLabel("Client");
            lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 13));
            panel_1.add(lblNewLabel);
            
            pathclient = new JTextField();
            pathclient.setEditable(false);
            panel_1.add(pathclient);
            pathclient.setColumns(40);

            getContentPane().add(directoryPanel, BorderLayout.CENTER);
        }

        this.setVisible(true);

        String initPath = new File(".").getAbsolutePath();
        System.out.println();
        ftpManager = new FTPManager(serverDirectoryList, clientDirectoryList, initPath, msgField, pathclient, pathserver);

    }

    public MainFrame(String title){
        this();
        this.setTitle(title);
    }


    class DirectoryCellRenderer extends JLabel implements ListCellRenderer {
    	
        private final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);
        

        public DirectoryCellRenderer() {
            setOpaque(true);
            setIconTextGap(12);
        }

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            DirectoryItem entry = (DirectoryItem) value;
            setText(entry.getTitle());
            setIcon(entry.getImage());
            if (isSelected) {
                setBackground(HIGHLIGHT_COLOR);
                setForeground(Color.white);
            } else {
                setBackground(Color.white);
                setForeground(Color.black);
            }
            
            return this;
        }
    }

}
