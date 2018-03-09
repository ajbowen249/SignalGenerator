import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.management.MBeanServer;
import javax.swing.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HMI implements Runnable {
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    private static final Pattern WINDOWS_COM_REGEX = Pattern.compile("Status for device (COM\\d+):");
    private static final String WINDOWS_LIST_PORTS = "cmd /c mode";
    private static final String WINDOWS_SET_UP_PORT = "cmd /c mode %s:9600,N,8,1,P";

    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
    private static final Pattern LINUX_USBTTY_REGEX = Pattern.compile("(ttyUSB\\d+)");
    private static final String LINUX_LIST_PORTS = "ls /dev";
    private static final String LINUX_SET_UP_PORT = "stty -F %s ixon -cstopb -parenb 9600";

    private static final String WINDOW_TITLE = "Signal Generator HMI";
    private static final String CONNECT_BUTTON = "Connect";
    private static final String DISCONNECT_BUTTON = "Disconnect";
    private static final String INITIAL_STATUS = "Started";
    private static final String SEND_BUTTON = "Send";

    private JFrame mMainWindow;
    private JComboBox mSerialPortField;
    private JButton mConnectButton;
    private JButton mDisconnectButton;
    private JLabel mStatusBar;
    private JLabel mIntervalOnDevice;
    private JLabel mFrequency;
    private JLabel mPeriod;
    private JTextField mIntervalField;
    private JButton mSendButton;

    private BufferedReader mInputStream;
    private FileOutputStream mOutputStream;

    @Override
    public void run() {
        mMainWindow = new JFrame(WINDOW_TITLE);
        mMainWindow.setSize(470, 160);
        mMainWindow.setResizable(false);
        mMainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mMainWindow.setLayout(new BorderLayout());

        mStatusBar = new JLabel(INITIAL_STATUS);
        mMainWindow.add(mStatusBar, BorderLayout.SOUTH);

        JPanel controlsBox = new JPanel();
        controlsBox.setLayout(new BoxLayout(controlsBox, BoxLayout.Y_AXIS));
        mMainWindow.add(controlsBox, BorderLayout.NORTH);

        JPanel connectBar = new JPanel();
        connectBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        controlsBox.add(connectBar, BorderLayout.NORTH);

        mSerialPortField = new JComboBox(getPorts());
        connectBar.add(mSerialPortField);

        mConnectButton = new JButton(CONNECT_BUTTON);
        connectBar.add(mConnectButton);
        mConnectButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) { connect(); }
            }
        );

        mDisconnectButton = new JButton(DISCONNECT_BUTTON);
        connectBar.add(mDisconnectButton);
        mDisconnectButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) { disconnect(); }
            }
        );

        JPanel liveBar = new JPanel();
        liveBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        controlsBox.add(liveBar);

        liveBar.add(new JLabel("Interval: "));
        mIntervalOnDevice = new JLabel();
        liveBar.add(mIntervalOnDevice);

        liveBar.add(new JLabel("Frequency: "));
        mFrequency = new JLabel();
        liveBar.add(mFrequency);

        liveBar.add(new JLabel("Period: "));
        mPeriod = new JLabel();
        liveBar.add(mPeriod);

        JPanel updateBar = new JPanel();
        updateBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        controlsBox.add(updateBar);

        ActionListener sendEventListner = new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { send(); }
        };

        mIntervalField = new JTextField("", 10);
        mIntervalField.addActionListener(sendEventListner);
        updateBar.add(mIntervalField);

        mSendButton = new JButton(SEND_BUTTON);
        updateBar.add(mSendButton);
        mSendButton.addActionListener(sendEventListner);

        disconnectedControls();

        mMainWindow.setVisible(true);
    }

    private void connect() {
        try {
            String portPath = (String)mSerialPortField.getSelectedItem();
            if(IS_LINUX) portPath = "/dev/" + portPath;
            setupPort(portPath);
            if(IS_WINDOWS) portPath = "\\\\.\\" + portPath;
            mInputStream = new BufferedReader(new InputStreamReader(new FileInputStream(portPath)));
            mOutputStream = new FileOutputStream(portPath);

            Thread.sleep(2000);

            setStatus("Connected Successfully!");
            connectedControls();
            getInterval();
        } catch (Exception e) {
            System.out.println("Error Connecting: " + e);
        }
    }

    private String[] getPorts() {
        try {
            if(IS_WINDOWS) {
                Process modeProc = Runtime.getRuntime().exec(WINDOWS_LIST_PORTS);
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(modeProc.getInputStream()));
                List<String> ports = new ArrayList<String>();

                String line;
                while((line = stdIn.readLine()) != null) {
                    Matcher comMatcher = WINDOWS_COM_REGEX.matcher(line);
                    if(comMatcher.matches()) {
                        ports.add(comMatcher.group(1));
                    }
                }

                return ports.toArray(new String[0]);
            } else if(IS_LINUX) {
                Process lsProc = Runtime.getRuntime().exec(LINUX_LIST_PORTS);
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(lsProc.getInputStream()));
                List<String> ports = new ArrayList<String>();

                String line;
                while((line = stdIn.readLine()) != null) {
                    Matcher ttyMatcher = LINUX_USBTTY_REGEX.matcher(line);
                    if(ttyMatcher.matches()) {
                        ports.add(ttyMatcher.group(1));
                    }
                }

                return ports.toArray(new String[0]);
            } else {
                setStatus("Error: unsupported platform.");
                return new String[] { };
            }
        } catch(IOException e) {
            setStatus("Error getting serial ports: " + e);
            return new String[] { };
        }
    }

    private void setupPort(String portName) {
        String errorPrefix = "Error setting up port: ";
        if(IS_WINDOWS) {
            try {
                String command = String.format(WINDOWS_SET_UP_PORT, portName);
                Runtime.getRuntime().exec(command);
            } catch (Exception e) {
                System.out.println(errorPrefix + e);
            }

            return;
        }

        if(IS_LINUX) {
            try {
                String command = String.format(LINUX_SET_UP_PORT, portName);
                Runtime.getRuntime().exec(command);
            } catch (Exception e) {
                System.out.println(errorPrefix + e);
            }
        }
    }

    private void disconnect() {
        try {
            mInputStream.close();
        } catch (Exception ex) {
            setStatus("Disconnection error: " + ex);
        }

        disconnectedControls();
    }

    private void send() {
        String value = mIntervalField.getText();
        if(value.isEmpty()) {
            return;
        }

        try {
            mOutputStream.write(("#interval=" + value + ";").getBytes("ASCII"));
            getInterval();
        } catch (Exception ex) {
            setStatus("Connection error: " + ex);
        }
    }

    private void setStatus(String newStatus) {
        mStatusBar.setText(newStatus);
    }

    private void getInterval() {
        String interval = transact("#interval?");
        mIntervalOnDevice.setText(interval + "us");

        long parsed = Long.parseLong(interval);
        long period = parsed * 2;
        double frequency = 1d / ((double)period / 1000000d);

        mPeriod.setText("" + period + "us");
        mFrequency.setText("" + frequency + "hz");
    }

    private String transact(String command) {
        try{
            mOutputStream.write(command.getBytes("ASCII"));
            String response = "";
            while(response == null || response.isEmpty()) {
                response = mInputStream.readLine();
            }

            return response.replace("\r", "").replace("\n", "");
        } catch (Exception ex) {
            setStatus("Communication error: " + ex);
        }

        return null;
    }

    private void disconnectedControls() {
        mSendButton.setEnabled(false);
        mIntervalField.setEnabled(false);
        mDisconnectButton.setEnabled(false);

        mConnectButton.setEnabled(true);
    }

    private void connectedControls() {
        mSendButton.setEnabled(true);
        mIntervalField.setEnabled(true);
        mDisconnectButton.setEnabled(true);

        mConnectButton.setEnabled(false);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        HMI app = new HMI();
        SwingUtilities.invokeLater(app);
    }
}
