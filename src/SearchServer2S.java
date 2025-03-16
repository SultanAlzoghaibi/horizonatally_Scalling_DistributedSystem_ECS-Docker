import javax.xml.transform.Source;
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
        portNumIncrement = 0;


        //
        /*for (int i = 0; i < server2dChar.length; i++) {
            for (int j = 0; j < server2dChar[i].length; j++) {
                server2dChar[i][j] = ' ';
            }
        }*/


        try{
            ss = new ServerSocket(30000);
        } catch(IOException e){
            System.err.println("Port 30000 already in use, pick another port or close the running instance.");
            e.printStackTrace();
            System.exit(1);
        }
        // From chatGTP as a way to close the socket if inteliji/Java does nto do it automatically
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(ss != null && !ss.isClosed()){
                    ss.close();
                    System.out.println("Server socket closed gracefully.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        // End of chatGTP
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
            System.out.print("PlayerData: ");
            System.out.print("User ID: " + playerData.getUserId() + ", ");
            System.out.print("Username: " + playerData.getUsername() + ", ");
            System.out.print("ELO: " + playerData.getElo() + " | ");

            System.out.print("Connection: ");
            System.out.print("Player ID: " + ssc.getPlayerID() + ", ");
            System.out.println("Socket: " + ssc.getSocketPort());
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
                try {
                    while (true) {


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
                                        System.out.println("matchMakingToElo()");
                                        Thread.sleep(100); // Wait 10 seconds for players to join
                                        matchMakingToElo();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            t.start();

                    }
                } catch (Exception e) {
                    System.out.println("ClassNotFoundException");
                }


            } catch (IOException e) {
                System.out.println("IOException from run() : ServerSideConnection");
            }
        }

        public void matchMakingToElo(){

            if(sscPlayerDataArrayList.size() >= 2){
                System.out.println("sscPlayerDataArrayList >= 2");
                SscPlayerData player1 = sscPlayerDataArrayList.removeFirst();
                SscPlayerData player2 = sscPlayerDataArrayList.removeFirst();
                portNumIncrement++;
                //portNumIncrement = 1;
                int portNumber = 30000 + portNumIncrement;
                String strPortNumber = Integer.toString(portNumber);

                //ASKED chatGTP fro porces builder file path ans
                ProcessBuilder pb = new ProcessBuilder(
                        "java",
                        "-cp",
                        "/Users/sultan/Desktop/seng-300/JavaWebSockets/out/production/JavaWebSockets",
                        "GameServer2ST",
                        strPortNumber
                );

                try {
                    Process process = pb.start(); // storing the process but might not use tho.
                    System.out.println("Launched GameServer2ST on port: " + strPortNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //End of chatGTP

                player1.getSsc().sendserverPortNumber(portNumber);
                player2.getSsc().sendserverPortNumber(portNumber);
                System.out.println( "sent them port num: " + portNumber);
                System.exit(0);

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

        private int getPlayerID(){
            return playerID;
        }

        private int getSocketPort(){
            return socket.getLocalPort();
        }



    }

    public void closeServer() {
        try {
            if (ss != null && !ss.isClosed()) {
                ss.close();
                System.out.println("Server socket closed.");
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        SearchServer2S searchS = new SearchServer2S();

        // Add shutdown hook to ensure the server socket closes when stopped
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown detected. Closing server socket...");
            searchS.closeServer();
        }));

        try {
            searchS.acceptConnections();
        } catch (Exception e) {
            System.out.println("Server shutting down...");
        } finally {
            searchS.closeServer();
        }
    }
}
