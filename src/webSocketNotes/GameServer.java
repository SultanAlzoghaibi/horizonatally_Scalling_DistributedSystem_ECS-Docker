package webSocketNotes;

import java.net.*;
import java.io.*;
import java.util.Arrays;

public class GameServer {
    private ServerSocket ss;
    private int numPlayers;
    private ServerSideConnection player1;
    // these a the server side equilivant to the package drop off pouints i think
    private ServerSideConnection player2;
    private int turnsMade;
    private int maxTurns;
    private int[] values;

    // store the  the button num that the player clicked on, befroe being sent to the other player
    // don in the run method while loop, for each turns
    private int player1ButtonNum;
    private int player2ButtonNum;

    private char[] gameBoard;

    public GameServer() {
        System.out.println("--game server--");
        numPlayers = 0;
        turnsMade = 0;
        maxTurns = 5;
        values = new int[4];

        for (int i = 0; i < 4; i++) { //Ading the values fromt he server not
            values[i] = i;
        }
        System.out.println("values array:" + Arrays.toString(values));



        try{
            ss = new ServerSocket(30000);
        } catch(IOException e){
            System.out.println("IOException from game server constructor");
            e.printStackTrace();
        }
    }


    public void acceptConnections(){

        try {
            System.out.println("waiting for connections");

            while (numPlayers < 2) {
                Socket s = ss.accept();
                numPlayers++;
                System.out.println("webSocketNotes.Player #" + numPlayers + " has joined the game");
                ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);
                if (numPlayers == 1) {
                    player1 = ssc;
                } else {
                    player2 = ssc;
                }

                Thread t = new Thread(ssc); //what ever is the in the ssc run in the new "THREAD"
                t.start();
            }
            System.out.println("2 player reach, no more looking for players");
        }catch(IOException e){
            System.out.println("IOException from game server acceptConnections");
        }
    }

    private class ServerSideConnection implements Runnable{
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private int playerID;


        public ServerSideConnection(Socket s, int id){
            socket = s;
            playerID = id;
            try {
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.out.println("IOException from game server constructor: ServerSideConnection");
            }
        }
        public void run(){ // insctruction we want to run on a NEW thread
            try {
                dataOut.writeInt(playerID);
                dataOut.writeInt(maxTurns);

                for (int i = 0; i < 4; i++) {
                    dataOut.writeInt(values[i]);
                }
                dataOut.flush();
                while (true) {
                    if(playerID == 1){
                        player1ButtonNum = dataIn.readInt(); // read it from player 1
                        System.out.println("Payer 1 clicked button #" + player1ButtonNum);
                        player2.sendButtonNum(player1ButtonNum);


                    }
                    else{
                        player2ButtonNum = dataIn.readInt();
                        System.out.println("Payer 2 clicked button #" + player2ButtonNum);
                        player1.sendButtonNum(player2ButtonNum);
                    }
                    turnsMade++;
                    if(turnsMade >= maxTurns){
                        System.out.println("Max turns reached");
                        break; // break from the game and end
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException from run() : ServerSideConnection");
            }
        }

        public void sendButtonNum(int buttonNum){
            try{
                dataOut.writeInt(buttonNum);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from sendButtonNum() : ServerSideConnection");
            }
        }

    }


    public static void main(String[] args) {
        GameServer gs = new GameServer();
        gs.acceptConnections();

    }
}
