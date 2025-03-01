
import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.awt.event.*;

public class Player extends JFrame {


    private int width, height;
    private Container contentPane;
    private JTextArea message;
    private JButton b1,b2,b3,b4;
    private ClientSideConnection csc;
    private int playerID;
    private int otherPlayerID;

    private int[] values;
    private int maxTurns;
    private int turnsMade;
    private int myPoints;
    private int enemyPoints;



    public Player(int w, int h) {
        width = w;
        height = h;
        contentPane = this.getContentPane();
        message = new JTextArea();
        b1 = new JButton("1");
        b2 = new JButton("2");
        b3 = new JButton("3");
        b4 = new JButton("4");

        values = new int[4];
        turnsMade = 0;
        myPoints = 0;
        enemyPoints = 0;


    }

    public void setUpGUII(){
        this.setSize(width, height);
        this.setTitle("the Game for Player #" + playerID);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contentPane.setLayout(new GridLayout(1,5));
        contentPane.add(message);
        message.setText("Turn based game");
        message.setWrapStyleWord(true);
        message.setLineWrap(true);
        message.setEditable(false);
        contentPane.add(b1);
        contentPane.add(b2);
        contentPane.add(b3);
        contentPane.add(b4);

        if (playerID == 1){
            message.setText("you are player 1, you go first");
        }
        else {
            message.setText("you are player 2, wait for your turn");
        }

        this.setVisible(true); // set frame visibility true
    }

    public void connectToServer(){
        csc = new ClientSideConnection();
    }
    public void setUpButtons(){
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JButton b = (JButton) ae.getSource();
               int bNum = Integer.parseInt(b.getText()); // this is a string lets parse it
                message.setText(" You clicked button #" + bNum + "now wait fo next player turn") ;

                turnsMade++;
                System.out.println("Turns made: " + turnsMade);
                myPoints = myPoints + values[bNum];
                System.out.println("myPoints: " + myPoints);
            }
        };
        b1.addActionListener(al); // ADDING ALL THE BUTTONS
        b2.addActionListener(al);
        b3.addActionListener(al);
        b4.addActionListener(al);

    }

    //Networking instruction for the Client
    private class ClientSideConnection{
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        public ClientSideConnection(){
            System.out.println("Client side connection");


            try{
                socket = new Socket("localhost", 30000);
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
                playerID = dataIn.readInt();
                System.out.println("Player ID: " + playerID);

                //VALUES TO SEND OVER THE NETWORK!
                maxTurns = dataIn.readInt() / 2;
                values[0] = dataIn.readInt();
                values[1] = dataIn.readInt();
                values[2] = dataIn.readInt();
                values[3] = dataIn.readInt();

                System.out.println("maxTurns: " + maxTurns);
                System.out.println("values[0]: " + values[0]);
                System.out.println("values[1]: " + values[1]);
                System.out.println("values[2]: " + values[2]);
                System.out.println("values[3]: " + values[3]);



            }catch (Exception e){
                System.out.println("IO exception from CSC contructor");

            }
        }
    }

    public static void main(String[] args) {
        Player p = new Player(800, 600);
        p.connectToServer();
        p.setUpGUII();
        p.setUpButtons();
    }
}