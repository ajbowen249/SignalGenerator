import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.management.MBeanServer;
import javax.swing.*;

import javafx.event.ActionEvent;

import jssc.*;

public class HMI implements Runnable {
    private static final String WINDOW_TITLE = "Signal Generator HMI";
    private static final String CONNECT_BUTTON = "Connect";
    private static final String DISCONNECT_BUTTON = "Disconnect";
    private static final String INITIAL_STATUS = "Started";
    private static final String SEND_BUTTON = "Send";

    private static final int DEFAULT_BAUDRATE = SerialPort.BAUDRATE_9600;

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

    private SerialPort mSerialPort;

    @Override
    public void run() {
        mMainWindow = new JFrame(WINDOW_TITLE);
        mMainWindow.setSize(470, 160);
        mMainWindow.setResizable(false);
        mMainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mMainWindow.setLayout(new BorderLayout());

        JPanel controlsBox = new JPanel();
        controlsBox.setLayout(new BoxLayout(controlsBox, BoxLayout.Y_AXIS));
        mMainWindow.add(controlsBox, BorderLayout.NORTH);

        JPanel connectBar = new JPanel();
        connectBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        controlsBox.add(connectBar, BorderLayout.NORTH);

        mSerialPortField = new JComboBox(SerialPortList.getPortNames());
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

        mStatusBar = new JLabel(INITIAL_STATUS);
        mMainWindow.add(mStatusBar, BorderLayout.SOUTH);

        disconnectedControls();

        mMainWindow.setVisible(true);
    }

    private void connect() {
        try {
            mSerialPort = new SerialPort((String)mSerialPortField.getSelectedItem());
            mSerialPort.openPort();

            mSerialPort.setParams(DEFAULT_BAUDRATE,
                                  SerialPort.DATABITS_8,
                                  SerialPort.STOPBITS_1,
                                  SerialPort.PARITY_NONE
            );

            mSerialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            setStatus("Connected Successfully!");
            connectedControls();
            getInterval();
        } catch (SerialPortException ex) {
            setStatus("Connection error: " + ex);
        }
    }

    private void disconnect() {
        try {
            mSerialPort.closePort();
        } catch (SerialPortException ex) {
            setStatus("Connection error: " + ex);
        }

        disconnectedControls();
    }

    private void send() {
        String value = mIntervalField.getText();
        if(value.isEmpty()) {
            return;
        }

        try {
            mSerialPort.writeString("#interval=" + value + ";");
            getInterval();
        } catch (SerialPortException ex) {
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
            mSerialPort.writeString(command);
            String recieved = "";
            while(!recieved.endsWith("\n")) {
                String newValue = mSerialPort.readString();
                if(newValue != null) {
                    recieved += newValue;
                }
            }

            return recieved.replace("\r", "").replace("\n", "");
        } catch (SerialPortException ex) {
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
        HMI app = new HMI();
        SwingUtilities.invokeLater(app);
    }
}
