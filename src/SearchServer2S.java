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
        /*for (int i = 0; i < server2dChar.length; i++) {
            for (int j = 0; j < server2dChar[i].length; j++) {
                server2dChar[i][j] = ' ';
            }
        }*/



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

                dataOut.flush();
                    while (true) {
                        try {

                            tempPlayerData = (PlayerData) dataInObj.readObject();
                            tempPlayerData.setUserId(playerID);


                            playerDataArrayList.add(tempPlayerData);
                            System.out.println(" The playerDataArrayList: ");
                            for (PlayerData playerData : playerDataArrayList) {
                                playerData.printPlayerData();
                            }

                        } catch (Exception e) {
                            System.out.println("ClassNotFoundException");
                        }
                    }


            } catch (IOException e) {
                System.out.println("IOException from run() : ServerSideConnection");
            }
        }

    }




    public static void main(String[] args) {
        SearchServer2S searchS = new SearchServer2S();
        searchS.acceptConnections();

    }
}
