import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.management.MBeanServer;
import javax.swing.*;
import java.awt.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private static final String SET_BUTTON = "Set";
    private static final String SIGNAL_GENERATOR_BORDER = "Signal Generator";
    private static final String EEPROM_PROGRAMMER_BORDER = "EEPROM Programmer";
    private static final String READ_BUTTON = "Read";
    private static final String Write_BUTTON = "Write";

    private final EEPROMOption[] COMPATIBLE_EEPROM_TYPES = {
        new EEPROMOption("24-Pin Parallel", 0, 24),
        new EEPROMOption("28-Pin Parallel", 1, 28),
        new EEPROMOption("32-Pin Parallel", 2, 32),
    };

    private JFrame mMainWindow;

    private JComboBox<String> mSerialPortField;
    private JButton mConnectButton;
    private JButton mDisconnectButton;
    private JLabel mStatusBar;
    private JLabel mIntervalOnDevice;
    private JLabel mFrequency;
    private JLabel mPeriod;
    private JTextField mIntervalField;
    private JButton mSendButton;

    private JComboBox<EEPROMOption> mEEPROMTypeField;
    private JButton mReadButton;
    private JButton mWriteButton;
    private JProgressBar mEEPROProgressBar;

    private RandomAccessFile mPortFile;
    private BufferedReader mInputStream;
    private FileOutputStream mOutputStream;

    @Override
    public void run() {
        mMainWindow = new JFrame(WINDOW_TITLE);
        mMainWindow.setSize(470, 500);
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

        mSerialPortField = new JComboBox<>(getPorts());
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

        JPanel signalGenGroup = new JPanel();
        signalGenGroup.setLayout(new BoxLayout(signalGenGroup, BoxLayout.Y_AXIS));
        controlsBox.add(signalGenGroup, BorderLayout.NORTH);
        signalGenGroup.setBorder(BorderFactory.createTitledBorder(SIGNAL_GENERATOR_BORDER));

        JPanel liveBar = new JPanel();
        liveBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        signalGenGroup.add(liveBar);

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
        signalGenGroup.add(updateBar);

        ActionListener sendEventListner = new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { send(); }
        };

        mIntervalField = new JTextField("", 10);
        mIntervalField.addActionListener(sendEventListner);
        updateBar.add(mIntervalField);

        mSendButton = new JButton(SET_BUTTON);
        updateBar.add(mSendButton);
        mSendButton.addActionListener(sendEventListner);

        JPanel eepromGroup = new JPanel();
        eepromGroup.setLayout(new BorderLayout());
        controlsBox.add(eepromGroup, BorderLayout.NORTH);
        eepromGroup.setBorder(BorderFactory.createTitledBorder(EEPROM_PROGRAMMER_BORDER));

        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
        eepromGroup.add(selectionPanel, BorderLayout.WEST);

        mEEPROMTypeField = new JComboBox<>(COMPATIBLE_EEPROM_TYPES);
        mEEPROMTypeField.setSelectedIndex(1); // Default to 28 pin
        selectionPanel.add(mEEPROMTypeField);

        selectionPanel.add(new EEPROMPreview(mEEPROMTypeField));

        JPanel readWriteGroup = new JPanel();
        readWriteGroup.setLayout(new BorderLayout());
        eepromGroup.add(readWriteGroup, BorderLayout.CENTER);

        JPanel readWriteButtonsPanel = new JPanel();
        readWriteButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        readWriteGroup.add(readWriteButtonsPanel, BorderLayout.NORTH);

        mWriteButton = new JButton(Write_BUTTON);
        mWriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startEEPROMBurn();
            }
        });
        readWriteButtonsPanel.add(mWriteButton);

        mReadButton = new JButton(READ_BUTTON);
        readWriteButtonsPanel.add(mReadButton);

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        readWriteGroup.add(progressPanel);

        mEEPROProgressBar = new JProgressBar();
        progressPanel.add(mEEPROProgressBar);

        disconnectedControls();

        mMainWindow.setVisible(true);
    }

    private void connect() {
        try {
            String portPath = (String)mSerialPortField.getSelectedItem();
            if (IS_LINUX) portPath = "/dev/" + portPath;
            setupPort(portPath);
            if (IS_WINDOWS) portPath = "\\\\.\\" + portPath;

            if (IS_WINDOWS) {
                mPortFile = new RandomAccessFile(portPath, "rw");
            } else {
                mInputStream = new BufferedReader(new InputStreamReader(new FileInputStream(portPath)));
                mOutputStream = new FileOutputStream(portPath);
            }

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
            if (IS_WINDOWS || IS_LINUX) {
                Process proc = Runtime.getRuntime().exec(IS_WINDOWS ? WINDOWS_LIST_PORTS : LINUX_LIST_PORTS);
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                List<String> ports = new ArrayList<String>();

                String line;
                while((line = stdIn.readLine()) != null) {
                    Matcher listMatcher = (IS_WINDOWS ? WINDOWS_COM_REGEX : LINUX_USBTTY_REGEX).matcher(line);
                    if (listMatcher.matches()) {
                        ports.add(listMatcher.group(1));
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
        String command = IS_WINDOWS ? String.format(WINDOWS_SET_UP_PORT, portName) : String.format(LINUX_SET_UP_PORT, portName);
        try {
            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            System.out.println(errorPrefix + e);
        }
    }

    private void disconnect() {
        try {
            if (IS_WINDOWS) {
                mPortFile.close();
            } else {
                mInputStream.close();
            }
        } catch (Exception ex) {
            setStatus("Disconnect error: " + ex);
        }

        disconnectedControls();
    }

    private void send() {
        String value = mIntervalField.getText();
        if (value.isEmpty()) {
            return;
        }

        try {
            write("#interval=" + value + ";");
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
        try {
            byte[] test = command.getBytes("ASCII");
            return transact(test);
        } catch (UnsupportedEncodingException ex) {
            setStatus("Communication error: " + ex);
        }

        return "";
    }

    private String transact(byte[] command) {
        try{
            write(command);
            return readLine();
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
        mWriteButton.setEnabled(true);
        mReadButton.setEnabled(true);
        mEEPROMTypeField.setEnabled(true);

        mConnectButton.setEnabled(false);
    }

    private void rwControls() {
        mWriteButton.setEnabled(false);
        mReadButton.setEnabled(false);
        mEEPROMTypeField.setEnabled(false);
    }

    private void write(String command) throws IOException {
        write(command.getBytes("ASCII"));
    }

    private void write(byte[] command) throws IOException {
        if (IS_WINDOWS) {
            mPortFile.write(command);
        } else {
            mOutputStream.write(command);
        }
    }

    private String readLine() throws IOException {
        String response = "";
        while(response == null || response.isEmpty()) {
            if (IS_WINDOWS) {
                response = mPortFile.readLine();
            } else {
                response = mInputStream.readLine();
            }
        }

        return response.replace("\r", "").replace("\n", "");
    }

    private void startEEPROMBurn() {
        JFileChooser chooser = new JFileChooser();
        if(chooser.showOpenDialog(mMainWindow) == JFileChooser.APPROVE_OPTION) {
            final File inputFile = chooser.getSelectedFile();
            SwingWorker<Void, Void> writeWorker = new SwingWorker<Void,Void>(){
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        rwControls();
                        byte[] fileContents = Files.readAllBytes(Paths.get(inputFile.getAbsolutePath()));
                        int length = fileContents.length;

                        mEEPROProgressBar.setValue(0);
                        mEEPROProgressBar.setMinimum(0);
                        mEEPROProgressBar.setMaximum(length);

                        int chipType = ((EEPROMOption)mEEPROMTypeField.getSelectedItem()).getOption();
                        write("#initializeBurn(" + length + "," + chipType + ")");

                        for(int i = 0; i < length; i++) {
                            write("#w(");
                            write(new byte[]{ fileContents[i] });
                            transact(")");
                            mEEPROProgressBar.setValue(i);
                        }
                    } catch(Exception e) {
                        setStatus("Error reading ROM file: " + e.getMessage());
                    }

                    connectedControls();

                    return null;
                }
            };

            writeWorker.execute();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        HMI app = new HMI();
        SwingUtilities.invokeLater(app);
    }

    class EEPROMOption {
        private String mName;
        private int mOption;
        private int mPins;

        public EEPROMOption(String name, int option, int pins) {
            mName = name;
            mOption = option;
            mPins = pins;
        }

        @Override
        public String toString() {
            return mName;
        }

        int getPins() {
            return mPins;
        }

        int getOption() {
            return mOption;
        }
    }

    class EEPROMPreview extends JPanel {
        public static final long serialVersionUID = 0;
        private final Color PROGRAMMER_COLOR = new Color(0, 128, 0);
        private final int PROGRAMMER_HEIGHT = 250;
        private final int PROGRAMMER_WIDTH = 130;
        private final int PROGRAMMER_CORNER_DAIMETER = 15;
        private final int PIN_ROWS = 16;
        private final int PIN_WIDTH = PROGRAMMER_WIDTH / 3;
        private final int PIN_MARGIN_X = PROGRAMMER_WIDTH / 9;
        private final int PIN_MARGIN_Y = PROGRAMMER_HEIGHT / 20;
        private final int PIN_HEIGHT = (PROGRAMMER_HEIGHT - (PIN_MARGIN_Y * 2)) / (PIN_ROWS);
        private final int PIN_CELL_HEIGHT = (PROGRAMMER_HEIGHT - (PIN_MARGIN_Y * 2)) / (PIN_ROWS + 3);
        private final int PIN_INTERIM = ((PROGRAMMER_HEIGHT / PIN_ROWS) - PIN_CELL_HEIGHT) / 2;
        private final int PIN_BOX_HEIGHT = PIN_CELL_HEIGHT - (PIN_INTERIM * 2);
        private final int PIN_SLOT_MARGIN_Y = PIN_BOX_HEIGHT / 3;
        private final int PIN_SLOT_HEIGHT = PIN_BOX_HEIGHT - (PIN_SLOT_MARGIN_Y * 2);
        private final int PIN_SLOT_MARGIN_X = PIN_WIDTH / 9;
        private final int PIN_SLOT_WIDTH = PIN_WIDTH - (PIN_SLOT_MARGIN_X * 2);
        private final Color PIN_SLOT_COLOR = new Color(128, 128, 0);
        private final Color PIN_LEAD_COLOR = new Color(150, 150, 150);
        private final int PIN_LEAD_HEIGHT = PIN_BOX_HEIGHT / 2;
        private final int PIN_LEAD_MARGIN_X = PIN_SLOT_WIDTH / 5;
        private final int PIN_LEAD_WIDTH = PIN_WIDTH - PIN_LEAD_MARGIN_X;
        private final Color PACKAGE_COLOR = new Color(25, 25, 25);
        private final int PACKAGE_PADDING = 5;
        private final int NOTCH_DIAMETER_DENOMINATOR = 3;
        private final Color NOTCH_COLOR = new Color(75, 75, 75);

        private int mNumICPins = 24;

        public EEPROMPreview(JComboBox<EEPROMOption> selector) {
            super();
            setPreferredSize(new Dimension(PROGRAMMER_WIDTH, PROGRAMMER_HEIGHT));
            ActionListener listener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setNumPins(((EEPROMOption)selector.getSelectedItem()).getPins());
                }
            };
            selector.addActionListener(listener);

            // initialize pin count
            listener.actionPerformed(null);
        }

        public void setNumPins(int numPins) {
            mNumICPins = numPins;
            repaint();
            revalidate();
        }

        public void paintComponent(Graphics g) {
            g.setColor(PROGRAMMER_COLOR);
            g.fillRoundRect(0, 0, PROGRAMMER_WIDTH, PROGRAMMER_HEIGHT, PROGRAMMER_CORNER_DAIMETER, PROGRAMMER_CORNER_DAIMETER);

            int leftX = PIN_MARGIN_X;
            int rightX = PROGRAMMER_WIDTH - PIN_MARGIN_X - PIN_WIDTH;
            int leftSlotX = PIN_MARGIN_X + PIN_SLOT_MARGIN_X;
            int rightSlotX = rightX + PIN_SLOT_MARGIN_X;
            int leftLeadX = leftX + PIN_LEAD_MARGIN_X;
            int rightLeadX = rightX;
            int leadBubbleWidth = PIN_LEAD_WIDTH - (PIN_LEAD_WIDTH / 5);
            int leftLeadX2 = leftLeadX + PIN_LEAD_WIDTH / 5;

            int leadsStart = PIN_ROWS - (mNumICPins / 2);
            int packageStartY = 0;
            int packageEndY = 0;

            for (int i = 0; i < PIN_ROWS; i++) {
                g.setColor(PROGRAMMER_COLOR);
                int y = ((i * PIN_HEIGHT) + PIN_INTERIM + PIN_MARGIN_Y);

                g.draw3DRect(leftX, y, PIN_WIDTH, PIN_BOX_HEIGHT, false);
                g.draw3DRect(rightX, y, PIN_WIDTH, PIN_BOX_HEIGHT, false);

                g.setColor(PIN_SLOT_COLOR);
                g.fillRect(leftSlotX, y + PIN_SLOT_MARGIN_Y + 1, PIN_SLOT_WIDTH, PIN_SLOT_HEIGHT);
                g.fillRect(rightSlotX, y + PIN_SLOT_MARGIN_Y + 1, PIN_SLOT_WIDTH, PIN_SLOT_HEIGHT);

                if (i >= leadsStart) {
                    if (packageStartY == 0) {
                        packageStartY = y;
                    }

                    g.setColor(PIN_LEAD_COLOR);
                    int leadY = y + (PIN_BOX_HEIGHT - PIN_LEAD_HEIGHT) / 2;
                    g.fillRect(leftLeadX, leadY, PIN_LEAD_WIDTH, PIN_LEAD_HEIGHT);
                    g.fillRect(leftLeadX2, y, leadBubbleWidth, PIN_BOX_HEIGHT);

                    g.fillRect(rightLeadX, leadY, PIN_LEAD_WIDTH, PIN_LEAD_HEIGHT);
                    g.fillRect(rightLeadX, y, leadBubbleWidth, PIN_BOX_HEIGHT);
                }

                if (i == PIN_ROWS - 1) {
                    packageEndY = y + PIN_BOX_HEIGHT;
                }
            }

            g.setColor(PACKAGE_COLOR);
            int packageMargin = leadBubbleWidth / 4;
            int packageX = leftLeadX2 + packageMargin;
            int packageWidth = (rightLeadX + (leadBubbleWidth - packageMargin)) - packageX;
            int packageY = packageStartY - PACKAGE_PADDING;
            int packageHeight = (packageEndY - packageStartY) + (PACKAGE_PADDING * 2);
            g.fill3DRect(packageX, packageY, packageWidth, packageHeight, true);

            g.setColor(NOTCH_COLOR);
            int diameter = packageWidth / NOTCH_DIAMETER_DENOMINATOR;
            int halfDiameter = diameter / 2;
            g.drawArc(packageX + (packageWidth / 2) - halfDiameter, packageY - halfDiameter, diameter, diameter, 180, 180);
        }
    }
}
