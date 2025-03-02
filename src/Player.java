
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
    private boolean buttonsEnabled;
    // this will impliment the turn based aspect forcing the player
    // to wait the other players turn



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
            otherPlayerID = 2;
            buttonsEnabled = true;
        }
        else {
            message.setText("you are player 2, wait for your turn");
            otherPlayerID = 1;
            buttonsEnabled = false;
             /* why thread? bc we don't want the net code to interrupt the gui display
            every read can interrupt the code sequence until you receive the request
            Hence you ruin a thread and don't interpret the gui field like messages or toggle button
            */
            Thread t = new Thread(new Runnable() {
                public void run() {
                    updateTurn();
                }
            });
            t.start();
        }

        toggleButtons();

        this.setVisible(true); // set frame visibility true
    }

    public void toggleButtons(){
        b1.setEnabled(buttonsEnabled);
        b2.setEnabled(buttonsEnabled);
        b3.setEnabled(buttonsEnabled);
        b4.setEnabled(buttonsEnabled); // flips the buttons from javax.swing

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

                buttonsEnabled = false;
                toggleButtons();


                myPoints = myPoints + values[bNum-1];
                System.out.println("myPoints: " + myPoints);

                // NOTE!: csc is our communication tool with the Server,
                // like the mail drop off point
                csc.sendButtonNum(bNum);

                if (playerID == 2 && turnsMade == maxTurns){
                    checkWinner();
                } else {
                    // When you press uour turn, you now need to Wait for your next turn
                    Thread t = new Thread(new Runnable() {

                        public void run() {
                            updateTurn();
                        }

                    });
                    t.start();
                }
            }
        };
        b1.addActionListener(al); // ADDING ALL THE BUTTONS
        b2.addActionListener(al);
        b3.addActionListener(al);
        b4.addActionListener(al);

    }


    public void updateTurn(){
        int n = csc.receiveButtonNum();
        message.setText("your opponent clicked #" + n + "now your Turn");
        enemyPoints += values[n-1];
        System.out.println("Your enemy has " + enemyPoints + " points");

        if(playerID == 1 && turnsMade == maxTurns){ // win checker for player 1
            checkWinner();
        } else {
            buttonsEnabled = true;
        }

        toggleButtons();
    }

    private void checkWinner(){
        buttonsEnabled = false;
        if (myPoints > enemyPoints) {
            message.setText("You won! You: " + myPoints + " points, Enemy: " + enemyPoints + " points");
        } else if (myPoints < enemyPoints) {
            message.setText("You lost! You: " + myPoints + " points, Enemy: " + enemyPoints + " points");
        } else {
            message.setText("It's a tie! Both have " + myPoints + " points.");
        }

        csc.closeConnection();

    }

    //Networking instruction for the Client
    private class ClientSideConnection{
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        public ClientSideConnection(){   // I think this is waht is being send to the server
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

        public void sendButtonNum(int buttonNum){
            try{
                dataOut.writeInt(buttonNum); // this is sending to server an int
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection sendButtonNum");
            }

        }
        public int receiveButtonNum(){ // this is gonna read th int sent by
            // server about the other player num
            int n = -1; // placeholder n gets replaced with button ums 1-4

            try{
                n = dataIn.readInt();
                System.out.println("player #" + otherPlayerID + "clicked button #" + n );
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection receiveButtonNum");
            }
            return n;

        }

        public void closeConnection(){
            try {
                socket.close();
                System.out.println("Closing connection");

            } catch (IOException e){
                System.out.println("IO exception in ClientSideConnection closeConnection");
            }
        }


    }


    public static void main(String[] args) {
        Player p = new Player(800, 600);
        p.connectToServer();
        p.setUpGUII(); //has startReceivingButtonNums in it
        p.setUpButtons();
    }
}