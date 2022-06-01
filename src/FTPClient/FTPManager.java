package FTPClient;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPManager {

    private JTextPane msgField;
    private JTextField pathclient;
    private JTextField printserver;

    private JList<DirectoryItem> serverDirectoryList;
    private DirectoryItem[] serverDirectoryItems; 
    private MainFrame frame;
    private ArrayList<String> pathserverarray = new ArrayList<String>();
    int num = 0;

    //client
    private JList<DirectoryItem> clientDirectoryList; 
    private DirectoryItem[] clientDirectoryItems; 
    private String clientDirPath; 

    private Socket socket = new Socket(); 
    private BufferedReader ftpIn; 
    private PrintWriter printWriter;

    public FTPManager(JList<DirectoryItem> serverDirectoryList, JList<DirectoryItem> clientDirectoryList, String initPath) {
        this.clientDirPath = initPath;
        this.serverDirectoryList = serverDirectoryList;
        this.serverDirectoryItems = new DirectoryItem[0];
        this.clientDirectoryList = clientDirectoryList;
        this.clientDirectoryItems = new DirectoryItem[0];
        getClientDirectoryList(); 
    }


    public FTPManager(JList<DirectoryItem> serverDirectoryList, JList<DirectoryItem> clientDirectoryList, String initPath, JTextPane field, JTextField client, JTextField server1) {
        this(serverDirectoryList, clientDirectoryList, initPath);
        this.msgField = field;
        this.pathclient = client;
        this.printserver = server1;
    }
    
    public void printpathserver(String path) {
    	if(path != "../") {
    		num++;
    		pathserverarray.add("/"+path);
    		String arraypath = Arrays.toString(pathserverarray.toArray()) .replaceAll("[\\[\\]]", "");
    		String pathserver  = arraypath.replaceAll(",", "");
    		String printpathserver  = pathserver.replaceAll(" ", "");
    		printserver.setText(printpathserver);
    	} else {
    		num--;
    		pathserverarray.remove(num);
    		String arraypath = Arrays.toString(pathserverarray.toArray()) .replaceAll("[\\[\\]]", "");
    		String pathserver  = arraypath.replaceAll(",", "");
    		String printpathserver  = pathserver.replaceAll(" ", "");
    		printserver.setText(printpathserver);
    	}
    }

    public void printpathclient(String path) {
    	if(pathclient != null) {
            pathclient.setText(path);
    	} 
    }

    public void addTextToMsgField(String text) {
        if (msgField != null) { 
            if(text.startsWith("Server:")){
                appendToPane(msgField, text + "\n", new Color(240, 80, 80));
            }else if(text.startsWith("Client:")){ 
                appendToPane(msgField, text + "\n", new Color(80, 80, 240)); 
            }else{ 
                appendToPane(msgField, text + "\n", Color.black); 
            }
            msgField.setCaretPosition(msgField.getDocument().getLength()); 
        }
    }

 
    private void appendToPane(JTextPane tp, String msg, Color c)
    {
        StyleContext sc = StyleContext.getDefaultStyleContext(); 
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c); 

        int len = tp.getDocument().getLength(); 
        tp.setCaretPosition(len); 
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg); 
    }


    public void connectFTPServer(String host, String id, String pw, String port) {

        new Thread() {
            @Override
            public void run() {
                try {
                    quitServer();
                    if (socket.isConnected()) { 
                        socket.close();
                    }


                    int portNumber = 21;
                    try {
                        portNumber = (port == null || "".equals(port)) ? (21) : (Integer.parseInt(port));
                    } catch (Exception e) {
                        addTextToMsgField("Port phải là một số!");
                        return;
                    }

                    try {
                        addTextToMsgField("Client: Kết nối tới ftp://" + host + ":" + portNumber);
                        printpathserver("");
                        socket = new Socket(host, portNumber);
                    } catch (Exception e) {
                        addTextToMsgField("Không thể kết nối máy chủ!");
                        return;
                    }

                    try {
                        ftpIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                        printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"));
                    } catch (IOException io) { 
                        addTextToMsgField("Kết nối bị lỗi, đóng kết nối.");
                        socket.close();
                        ftpIn.close();
                        printWriter.close();
                        return;
                    }

                    handleMultiLineResponse();

                    loginToServer(id, pw);


                } catch (Exception e) {
                    addTextToMsgField("Kết nối bị lỗi, đóng kết nối.");
                }

            }

        }.start();


    }


    private void loginToServer(String id, String pw) {

        if (id == null || "".equals(id)) { 
            id = "anonymous";
        }

        String userName = "USER " + id;
        send(userName); 
        String resultCode = handleMultiLineResponse(); 

        if (resultCode.startsWith("331 ")) {
            String userPassword = "PASS " + pw;
            send(userPassword, true); 
            resultCode = handleMultiLineResponse();
        }


        if (resultCode.startsWith("230")) { 
            send("SYST"); 
            String os = handleMultiLineResponse(); 

            getServerDirectoryList(); 
        } else {

        }

    }


    private void getServerDirectoryList() {
        new Thread() {
            @Override
            public void run() {

                try {
                    if (!socket.isClosed() && socket.isConnected()) {
                        send("PASV"); 
                        String result = ftpIn.readLine(); 
                        if (result.startsWith("530 ")) {
                            addTextToMsgField("Server: Không thể thực hiện được lệnh.");
                            return;
                        } else {
                            addTextToMsgField("Server: " + result);
                        }


                        String[] results = result.split("\\("); 
                        String ip = getIp(results[1]); 
                        int port = getPortNum(results[1]); 

                        Socket dataConnection = new Socket();
                        try {
                            dataConnection = new Socket(ip, port); 
                            if (ftpIn.ready()) {
                                String code = ftpIn.readLine();
                                if (code.startsWith("425 ")) {
                                    addTextToMsgField("Không mở được kết nối truyền dữ liệu.1");
                                    return;
                                }
                            }

                            send("NLST"); 
                            handleResponse();


                            BufferedReader dataIn = new BufferedReader(new InputStreamReader(dataConnection.getInputStream(), "utf-8"));
                            ArrayList<DirectoryItem> list = new ArrayList<>(); 
                            String line;
                            while ((line = dataIn.readLine()) != null) {

                                list.add(new DirectoryItem(line));


                            }

                           
                            dataConnection.close();
                            dataIn.close();

                            handleResponse(); 

                            serverDirectoryItems = changeArrayListToStrings(list); 
                            serverDirectoryList.setListData(serverDirectoryItems); 
                           
                        } catch (IOException io) { 
                            addTextToMsgField("Truyền dữ liệu bị lỗi, đóng kết nối");
                            dataConnection.close();
                        } catch (Exception e) { 
                            addTextToMsgField("Không mở được kết nối truyền dữ liệu.2");
                        }

                    }
                } catch (IOException io) {
                    addTextToMsgField("Truyền dữ liệu bị lỗi, đóng kết nối");
                } catch (Exception e) {
                    addTextToMsgField("Không mở được kết nối truyền dữ liệu.\"");
                }
            }

        }.start();
    }


    private DirectoryItem[] changeArrayListToStrings(ArrayList<DirectoryItem> list) {
        DirectoryItem[] directoryItems = new DirectoryItem[list.size() + 1];
        directoryItems[0] = DirectoryItem.getPreDirectory(); 
        for (int i = 0; i < list.size(); i++) {
            directoryItems[i + 1] = list.get(i);
        }

        return directoryItems;
    }


    public void selectServerListItem(int index) {

        new Thread() {
            @Override
            public void run() {

                DirectoryItem item = serverDirectoryItems[index];
                if (DirectoryItem.TYPE_FOLDER.equals(item.getType())) { 
                    enterDirectory(item.getTitle());
                } else { 
                    downloadFile(item.getTitle());
                }

            }
        }.start();

    }


    private void enterDirectory(String directory) {
    	printpathserver(directory);
        try {
            if (!socket.isClosed() && socket.isConnected()) {
                send("CWD " + directory); 
                String response = ftpIn.readLine(); 
                if (response.startsWith("250 ")) { 
                    addTextToMsgField("Server: " + response);
                } else {
                    addTextToMsgField("Server: " + response);
                    return;
                }

                getServerDirectoryList(); 
            } else {
                addTextToMsgField("Không thể thực hiện được lệnh.");
            }

        } catch (Exception e) {
            addTextToMsgField("Không thể thực hiện được lệnh.");
        }

    }

    private void getClientDirectoryList() {
        new Thread() {
            @Override
            public void run() {
                File dir = new File(clientDirPath);
                printpathclient(clientDirPath);
                File[] fileList = dir.listFiles();
                try{
                    ArrayList<DirectoryItem> list = new ArrayList<>();
                    for(int i=0; i<fileList.length; i++) {
                        File file = fileList[i];
                        String fileName = file.getName();
                        DirectoryItem Item = file.isDirectory() ? new DirectoryItem(fileName, "d") : new DirectoryItem(fileName);

                        list.add(Item);
                    }
                    clientDirectoryItems = changeArrayListToStrings(list);
                    clientDirectoryList.setListData(clientDirectoryItems);
                } catch (Exception e) {

                }
            }
        }.start();
    }


    private void enterClientDirectory(String directory) {
        try {
            File newPath = new File(this.clientDirPath + "/" + directory);
            this.clientDirPath = newPath.getCanonicalPath();
        } catch (Exception e) {
            
        }
        getClientDirectoryList();
    }


    public void selectClientListItem(int index) {
        new Thread() {
            @Override
            public void run() {
                DirectoryItem item = clientDirectoryItems[index];

                if (DirectoryItem.TYPE_FOLDER.equals(item.getType())) {
                    enterClientDirectory(item.getTitle());
                } else { 
                    uploadFile(item.getTitle());
                }

            }
        }.start();
    }

    public boolean clientRename(int index, String replaceName) {
        DirectoryItem item = clientDirectoryItems[index];
        String pathName = item.getTitle();
        boolean isMoved = false;

        if (replaceName == null || "".equals(replaceName)) {
            return false;
        }

        try {
            File originFile = new File(this.clientDirPath + "/" + pathName);
            String renamePath = this.clientDirPath + "/" + replaceName;
            if(!item.getType().equals(DirectoryItem.TYPE_FOLDER)) {
                renamePath += "." + item.getType();
            }
            
            File renameFIle = new File(renamePath);
            isMoved = originFile.renameTo(renameFIle);
            getClientDirectoryList();
        } catch (Exception e) {

        }

        return isMoved;
    }

    public boolean clientDelete(int index) {

        DirectoryItem item = clientDirectoryItems[index];
        String pathName = item.getTitle();
        boolean isDeleted = false;

        try {
            File deleteFile = new File(this.clientDirPath + "/" + pathName);
            isDeleted = deleteFile.delete();
            getClientDirectoryList();
        } catch (Exception e) {

        }

        return isDeleted;
    }

    public boolean makeClientDirectory(String name) {
        boolean isCreated = false;


        if(name == null || "".equals(name)){
            return false;
        }

        try {
            File createDir = new File(this.clientDirPath + "/" + name);
            isCreated = createDir.mkdir();
            getClientDirectoryList();
        } catch (Exception e) {

        }

        return isCreated;
    }


    private void downloadFile(String pathName) {

        try {

            if (!socket.isClosed() && socket.isConnected()) {

                send("TYPE I"); 
                String response = ftpIn.readLine(); 

                if (response.startsWith("530 ")) { 
                    addTextToMsgField("Server: Không thể thực hiện được lệnh.");
                    return;
                } else
                    addTextToMsgField("Server: " + response);

                send("SIZE " + pathName); 
                String result = ftpIn.readLine();
                if (result.startsWith("550 ")) { 
                    addTextToMsgField("Server: Kết nối tới file " + pathName + " thất bại");
                    return;
                }
                addTextToMsgField("Server: " + result);


                String res[] = result.split(" "); 
                int size = Integer.parseInt(res[1]);

                send("PASV");
                result = printAndReturnLastResponse(); 

                String[] results = result.split("\\("); 
                String Ip = getIp(results[1]);
                int portNum = getPortNum(results[1]);

                Socket dataConnection = new Socket();
                try {
                    dataConnection = new Socket(Ip, portNum); 
                    if (ftpIn.ready()) {
                        String code = ftpIn.readLine();
                        if (code.startsWith("425 ")) {
                            addTextToMsgField("Server: Truyền dữ liệu tới " + Ip + " ở cổng " + portNum + " thất bại.");
                            return;
                        }
                    }

                    BufferedInputStream dataIn = new BufferedInputStream(dataConnection.getInputStream()); 
                    send("RETR " + pathName); 
                    response = ftpIn.readLine(); 
                    if (response.startsWith("450 ")) { 
                        addTextToMsgField("Server: Kết nối tới file " + pathName + " thất bại.");
                        return;
                    } else
                        addTextToMsgField("Server: " + response);

                    byte readIn[] = new byte[size];
                    int read;
                    int offset = 0;
                    while ((read = dataIn.read(readIn, offset, readIn.length - offset)) != -1) { 
                        offset += read;
                        if (readIn.length - offset == 0) {
                            break;
                        }
                    }


                    try {
                      
                        File file = new File(clientDirPath + "/" + pathName);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(readIn);
                        fos.close();
                    } catch (IOException io) {
                        addTextToMsgField("Không thể ghi vào tệp");
                    }

                    dataConnection.close();
                    dataIn.close();
                    handleMultiLineResponse();

                    getClientDirectoryList(); 
                } catch (IOException io) {
                    addTextToMsgField("Truyền dữ liệu bị lỗi, đóng kết nối");
                    dataConnection.close();
                } catch (IllegalArgumentException e) {
                    addTextToMsgField("830 Truyền dữ liệu tới " + Ip + " ở cổng " + portNum + " thất bại.");
                }

            } else {
                addTextToMsgField("Không thể thực hiện được lệnh.");
            }

        } catch (Exception e) {
            addTextToMsgField("Không thể thực hiện được lệnh.");
        }

    }


    private void uploadFile(String fileName) {

        try {
            File file = new File(fileName);
            Socket dataConnection = new Socket();
            String Ip = "";
            int portNum = 0;
            try {

                FileInputStream fileIn = new FileInputStream(file);
                int fileSize = (int) file.length();
                byte content[] = new byte[fileSize];
                fileIn.read(content, 0, fileSize);

                send("TYPE I"); 
                String response = ftpIn.readLine();
                if (response.startsWith("530 ")) {
                    addTextToMsgField("Server: Không thể thực hiện được lệnh.");
                    return;
                } else
                    addTextToMsgField("Server: " + response);


                send("PASV"); 
                String result = printAndReturnLastResponse();

                String[] results = result.split("\\(");
                Ip = getIp(results[1]); 
                portNum = getPortNum(results[1]); 

                dataConnection = new Socket(Ip, portNum);
                if (ftpIn.ready()) {
                    String code = ftpIn.readLine();
                    if (code.startsWith("425 ")) {
                        addTextToMsgField("Server: Truyền dữ liệu tới " + Ip + " ở cổng " + portNum + " thất bại.");
                        return;
                    }
                }

                BufferedOutputStream dataOut = new BufferedOutputStream(dataConnection.getOutputStream());

                send("STOR " + fileName);
                handleMultiLineResponse(); 

                dataOut.write(content, 0, fileSize); 
                dataOut.flush();

                fileIn.close();
                dataOut.close();
                dataConnection.close();

                handleMultiLineResponse();
                
                getServerDirectoryList(); 

            } catch (FileNotFoundException e) {
                addTextToMsgField("Kết nối tới file " + fileName + " thất bại.");
            } catch (IOException io) {
                addTextToMsgField("835 Truyền dữ liệu bị lỗi, đóng kết nối");
                dataConnection.close();
            } catch (IllegalArgumentException i) {
                addTextToMsgField("Truyền dữ liệu tới " + Ip + " ở cổng " + portNum + " thất bại.");
            }
        }catch (Exception e){
            addTextToMsgField("Không thể thực hiện được lệnh.");
        }

    }

 
    public void rename(int index, String replaceName) {


        DirectoryItem item = serverDirectoryItems[index];
        String pathName = item.getTitle();


        if(replaceName == null || "".equals(replaceName)){
            return;
        }

        try{

            send("RNFR " + pathName); 
            String result = handleMultiLineResponse();

            if(result.startsWith("350 ")) { 

                if(item.getType().equals(DirectoryItem.TYPE_FOLDER)) 
                    send("RNTO " + replaceName);
                else 
                    send("RNTO " + replaceName + "." + item.getType()); 

                result = handleMultiLineResponse();

                if(result.startsWith("250 ")) 
                    getServerDirectoryList(); 
            }

        }catch (Exception e){
            addTextToMsgField("Không thể thực hiện được lệnh.");
        }

    }


    public void delete(int index) {

        DirectoryItem item = serverDirectoryItems[index];
        String pathName = item.getTitle();

        try{

            if(item.getType().equals(DirectoryItem.TYPE_FOLDER)){ 
                send("RMD " + pathName);
            }else{ 
                send("DELE " + pathName);
            }

            String result = handleMultiLineResponse(); 
            if(result.startsWith("250 ")) 
                getServerDirectoryList();


        }catch (Exception e){
            addTextToMsgField("Không thể thực hiện được lệnh.");
        }

    }


    public void makeServerDirectory(String name) {


        if(name == null || "".equals(name)){
            return;
        }

        try{

            send("MKD " + name);
            String result = handleMultiLineResponse(); 
            if(result.startsWith("257 ")) 
                getServerDirectoryList(); 

        }catch (Exception e){
            addTextToMsgField("Không thể thực hiện được lệnh.");
        }

    }

    public void quitServer(){

        try {
            if(socket != null && socket.isConnected()) {
                send("QUIT");
                handleMultiLineResponse();
                socket.close();
                printWriter.close();
                ftpIn.close();
            }
        }catch (Exception e){
            addTextToMsgField("Không thể thực hiện được lệnh.");
        }

    }

 
    private String getIp(String input) {
        String values[] = new String[10];

        Pattern pattern = Pattern.compile("\\d+"); 
        Matcher matcher = pattern.matcher(input);
        int i = 0;
        while (matcher.find()) { 
            values[i] = (matcher.group());
            i++;
        }
        return values[0] + "." + values[1] + "." + values[2] + "." + values[3];
    }


    private int getPortNum(String input) {
        int portNum;

        String values[] = new String[10];

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        int i = 0;
        while (matcher.find()) { 
            values[i] = (matcher.group());
            i++;
        }
        portNum = Integer.parseInt(values[4]) * 256 + Integer.parseInt(values[5]); 
        return portNum;
    }

    private void send(String command) {

        printWriter.print(command + "\r\n");
        printWriter.flush();

        addTextToMsgField("Client: " + command); 
    }

    private void send(String command, boolean hideContent) {

        printWriter.print(command + "\r\n");
        printWriter.flush();


        String msg = command;
        if (hideContent) { 
            msg = "";
            for (int i = 0; i < command.length(); i++) {
                msg += "*";
            }
        }

        addTextToMsgField("Client: " + msg);
    }

    private String handleMultiLineResponse() {
        try {
            String result;
            while (!(result = ftpIn.readLine()).matches("\\d\\d\\d\\s.*")) {
                addTextToMsgField(result);
            }
            addTextToMsgField("Server: " + result);

            return result;
        } catch (IOException e) {
            addTextToMsgField("Kết nối bị lỗi, đóng kết nối.");
            try {
                socket.close();
                ftpIn.close();
                printWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return "";
        }
    }

    private void handleResponse() {
        try {
            String result = ftpIn.readLine();

            result = result.replaceFirst("-", " ");
            String results[] = result.split(" ");
            int errorCode = Integer.parseInt(results[0]);
            switch (errorCode) {
                case 503:
                    result = "Đối số không hợp lệ";
                case 501:
                    result = "Đối số không hợp lệ";
            }
            addTextToMsgField("Server: " + result);


        } catch (IOException e) {
            addTextToMsgField("Kết nối bị lỗi, đóng kết nối.");
            try {
                socket.close();
                ftpIn.close();
                printWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private String printAndReturnLastResponse() {
        String result = "";
        try {

            while (!(result = ftpIn.readLine()).matches("\\d\\d\\d\\s.*")) {
                System.out.println("Server: " + result);
            }
            System.out.println("Server: " + result);

        } catch (IOException e) {
            System.out.println("825 Kết nối bị lỗi, đóng kết nối.");
            try {
                socket.close();
                ftpIn.close();
                printWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return result;
    }

}

