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
    private ArrayList<SscPlayerData> sscPlayerDataArrayList = new ArrayList<>();

    private char[][] server2dChar;
    private ArrayList<PlayerData> playerDataArrayList = new ArrayList<>();
    private PlayerData tempPlayerData;
    private int portNumIncrement;

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

        server2dChar = new char[3][3];


        playerDataArrayList = new ArrayList<>();
        portNumIncrement = 3;


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

    class SscPlayerData {
        PlayerData playerData;
        ServerSideConnection ssc;

        public SscPlayerData(PlayerData playerData, ServerSideConnection connection) {
            this.playerData = playerData;
            this.ssc = connection;
        }

        public void setPlayerData(PlayerData playerData) {
            this.playerData = playerData;
        }
        public PlayerData getPlayerData() {
            return playerData;
        }
        public ServerSideConnection getSsc() {
            return ssc;
        }
        public void setSsc(ServerSideConnection ssc) {
            this.ssc = ssc;
        }

        public void printConnectionDetails() {
            System.out.println("PlayerData: ");
            playerData.printPlayerData();
            System.out.println("Connection: " + ssc.toString());
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

    public class ServerSideConnection implements Runnable{
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
                            SscPlayerData tempsscPlayerData = new SscPlayerData(tempPlayerData, this);
                            sscPlayerDataArrayList.add(tempsscPlayerData);
                            tempsscPlayerData.printConnectionDetails();


                            System.out.println(" The sscPlayerDataArrayList: ");
                            for (SscPlayerData sscPlayerData : sscPlayerDataArrayList) {
                                sscPlayerData.printConnectionDetails();
                            }

                            Thread t = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        Thread.sleep(10000); // Wait 10 seconds for players to join
                                        matchMakingToElo();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            t.start();


                        } catch (Exception e) {
                            System.out.println("ClassNotFoundException");
                        }
                    }


            } catch (IOException e) {
                System.out.println("IOException from run() : ServerSideConnection");
            }
        }

        public void matchMakingToElo(){
            if(sscPlayerDataArrayList.size() > 2){

                SscPlayerData player1 = sscPlayerDataArrayList.removeFirst();
                SscPlayerData player2 = sscPlayerDataArrayList.removeFirst();

                portNumIncrement++;


            new Thread(() -> {

                        sendserverPortNumber(portNumIncrement);
                        sendserverPortNumber(portNumIncrement);


                });

            }
        }


        public void sendserverPortNumber(int portNum){
            try{
                dataOut.writeInt(portNum);
                dataOut.flush();
            } catch (Exception e) {
                System.out.println("Exception in sendserverPortNumber");
            }

        }
    }




    public static void main(String[] args) {
        SearchServer2S searchS = new SearchServer2S();
        searchS.acceptConnections();

    }
}
