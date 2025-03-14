import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;


public class SearchServer2S {
    private ServerSocket ss;
    private int numPlayers;
    private ServerSideConnection player1;
    // these a the server side equilivant to the package drop off pouints i think
    private ServerSideConnection player2;
    private int turnsMade;
    private int maxTurns;
    private int[] values;
    private char[][] server2dChar;
    private ArrayList<PlayerData> playerDataArrayList = new ArrayList<>();
    private PlayerData tempPlayerData;

    // store the  the button num that the player clicked on, befroe being sent to the other player
    // don in the run method while loop, for each turns
    private String player1ButtonNum;
    private String player2ButtonNum;

    private char[] gameBoard;

    public SearchServer2S() {
        System.out.println("--game server--");
        numPlayers = 0;
        turnsMade = 0;
        maxTurns = 90;
        values = new int[4];
        server2dChar = new char[3][3];
        playerDataArrayList = new ArrayList<>();

        for (int i = 0; i < 4; i++) { //Ading the values fromt he server not
            values[i] = i;
        }

        //
        for (int i = 0; i < server2dChar.length; i++) {
            for (int j = 0; j < server2dChar[i].length; j++) {
                server2dChar[i][j] = ' ';
            }
        }

        for (int i = 0; i < server2dChar.length; i++) {
            for (int j = 0; j < server2dChar[i].length; j++) {
                System.out.print("["+ server2dChar[i][j] + "]");
            }
            System.out.println();
        }




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

            while (numPlayers < 50) {
                Socket s = ss.accept();
                numPlayers++;
                System.out.println("webSocketNotes.Player #" + numPlayers + " has joined the game");
                ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);

                Thread t = new Thread(ssc); //what ever is the in the ssc run in the new "THREAD"
                t.start();
            }

        }catch(IOException e){
            System.out.println("IOException from game server acceptConnections");
        }
    }

    private class ServerSideConnection implements Runnable{
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private int playerID;
        private ObjectOutputStream dataOutObj;
        private ObjectInputStream dataInObj;


        public ServerSideConnection(Socket s, int id){
            socket = s;
            playerID = id;

            try {
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
                dataInObj = new ObjectInputStream(dataIn);
                dataOutObj = new ObjectOutputStream(dataOut);

            } catch (IOException e) {
                System.out.println("IOException from game server constructor: ServerSideConnection");
            }
        }
        public void run(){ // insctruction we want to run on a NEW thread
            try {
                dataOut.writeInt(playerID);
                dataOut.writeInt(maxTurns);

                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        dataOut.writeChar(server2dChar[i][j]);
                    }
                }
                dataOut.flush();

                    try {
                        PlayerData tempPlayerData = new PlayerData(1,"Na", 1);
                        //tempPlayerData = receivePlayerData();
                        tempPlayerData.printPlayerData();


                        playerDataArrayList.add(tempPlayerData);

                    } catch (Exception e) {
                        System.out.println("ClassNotFoundException");
                    }


            } catch (IOException e) {
                System.out.println("IOException from run() : ServerSideConnection");
            }
        }


        public PlayerData receivePlayerData() {
            try {
                ObjectInputStream objectIn = new ObjectInputStream(dataIn);
                return (PlayerData) objectIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("IO exception in receivePlayerData");
                return new PlayerData(0, "nA", 0);
            }
        }


        public void sendButtonNum(String buttonNum){
            try{
                dataOut.writeChars(buttonNum);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from sendButtonNum() : ServerSideConnection");
            }
        }

        public void send2dCharArray() {
            try {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        dataOut.writeChar(server2dChar[i][j]);

                    }
                }
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IOException from send2dCharArray() : ServerSideConnection");
            }
        }

        public boolean endGame(){
            return turnsMade >= maxTurns;
        }

            // I ASKED chatgtp to give be a better fromat instead of 2 sets fo 9 if statments
        public void processGameLogicP1(String input) {
            placeMove(input, 'X');  // P1 uses 'X'
        }

        public void processGameLogicP2(String input2) {
            placeMove(input2, 'O');  // P2 uses 'O'
        }

        private void placeMove(String input, char symbol) {
            if (symbol == 'O'){
                System.out.println("processGameLogic for player 2, input: " + input);
            }
            else {
                System.out.println("processGameLogic for player 1, input: " + input);
            }

            int move = Integer.parseInt(input) - 1; // Convert input to index (0-based)
            int row = move / 3;
            int col = move % 3;

            if (server2dChar[row][col] == ' ') { // Check if the cell is empty
                server2dChar[row][col] = symbol;
            } else {
                System.out.println("Invalid move! Cell already occupied.");
            }
        }
        // END of chatgtp tentious work



    }




    public static void main(String[] args) {
        SearchServer2S gs = new SearchServer2S();
        gs.acceptConnections();

    }
}
