import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class SearchServer2S {
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
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
    private HashMap<String, ArrayList<SscPlayerData>> gameQueues = new HashMap<>();

    // store the  the button num that the player clicked on, befroe being sent to the other player
    // don in the run method while loop, for each turns
    private String player1ButtonNum;
    private String player2ButtonNum;

    private char[] gameBoard;

    public SearchServer2S() {
        System.out.println("--search server--");
        numPlayers = 0;
        portNumIncrement = 0;

        gameQueues.put("chess", new ArrayList<SscPlayerData>() );
        gameQueues.put("connect4", new ArrayList<SscPlayerData>() );
        gameQueues.put("tictactoe", new ArrayList<SscPlayerData>() );
        gameQueues.put("checkers", new ArrayList<SscPlayerData>() );

        System.out.println("gameQueues:" + gameQueues.toString());

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

        public SscPlayerData(PlayerData playerData, Runnable connection) {
            this.playerData = playerData;
            this.ssc = (ServerSideConnection) connection;
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
            playerData.printPlayerData();

            System.out.print(" || Connection: ");
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


    public void startMatchMakingThreads(){
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Tick - -------------------");
            synchronized (gameQueues) {

                for (String gameModes : gameQueues.keySet()) {
                    Thread t = new Thread(() -> {
                        try {
                            System.out.println(gameModes + ": sscPlayerDataArrayList.size() = " + gameQueues.get(gameModes).size());
                            Thread.sleep(10);
                            ArrayList<SscPlayerData> sscPDArraylist = gameQueues.get(gameModes);
                            //gameModeMatchMakingToElo(sscPDArraylist); // Remmeber Arraylist are MUTABLE in java
                            //matchMakingToElo();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                    t.start();
                }
            }
            }, 0, 5, TimeUnit.SECONDS);
        };


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

                dataOutObj = new ObjectOutputStream(dataOut);
                dataInObj = new ObjectInputStream(dataIn);

                System.out.println("Connected to Search Server on port: " + s.getPort());

            } catch (IOException e) {
                System.out.println("IOException from game server constructor: ServerSideConnection");
            }
        }
        public void run() {

                        try {
                            // ISSUE, 1 thread per person can be inefeiccient
                            tempPlayerData = (PlayerData) dataInObj.readObject();

                            tempPlayerData.setUserId(playerID);
                            System.out.print("receice player data: ");
                            SscPlayerData tempsscPlayerData = new SscPlayerData(tempPlayerData, this);

                            tempsscPlayerData.printConnectionDetails();
                            System.out.print("Before gameQueues:");
                            printGameQueues();
                            String gameMode = tempsscPlayerData.playerData.getGameModeInterested();
                            gameQueues.get(gameMode).add(tempsscPlayerData);

                            System.out.print("After GameQueues: ");
                            printGameQueues();


                            Thread t = new Thread(() -> {
                                try {
                                    System.out.println("gameModeMatchMakingToElo(" + gameMode + ": sscPDArraylist)");
                                    Thread.sleep(1);
                                    gameModeMatchMakingToElo(gameMode);
                                    //matchMakingToElo();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });
                            t.start();

                        } catch (Exception e) {
                            System.out.println("ClassNotFoundException: " + e.getMessage());
                            ; //  Break the loop if an error occurs to avoid spamming
                            System.out.println("IOException from run() : ServerSideConnection " + e.getMessage());
            } finally {
                //closeConnection(); //  Ensure the socket is closed properly
            }
        }
        public void gameModeMatchMakingToElo(String gameMode){
            ArrayList<SscPlayerData> sscPDArraylist = gameQueues.get(gameMode);
            if(sscPDArraylist.size() >= 2){
                System.out.println("sscPlayerDataArrayList >= 2");
                SscPlayerData player1 = sscPDArraylist.removeFirst();
                SscPlayerData player2 = sscPDArraylist.removeFirst();

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
                        strPortNumber,
                        gameMode
                );

                try {
                    Process process = pb.start(); // storing the process but might not use tho.
                    System.out.println("Launched GameServer2ST on port: " + strPortNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //End of chatGTP
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                player1.getSsc().sendserverPortNumber(portNumber);
                player2.getSsc().sendserverPortNumber(portNumber);
                System.out.println("sent them port num: " + portNumber);
                //System.exit(0);
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
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                player1.getSsc().sendserverPortNumber(portNumber);
                player2.getSsc().sendserverPortNumber(portNumber);
                System.out.println( "sent them port num: " + portNumber);
                //System.exit(0);

            }
        }
        public void closeConnection() { // ask chatgtp to fill our this method
            try {
                if (dataInObj != null) {
                    dataInObj.close();
                }
                if (dataOutObj != null) {
                    dataOutObj.close();
                }
                if (dataIn != null) {
                    dataIn.close();
                }
                if (dataOut != null) {
                    dataOut.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                System.out.println("Connection closed for Player ID: " + playerID);
            } catch (IOException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }

        public void printGameQueues() {
            System.out.print("{");
            for (String gameQueue : gameQueues.keySet()) {
                System.out.print(gameQueue + ": " + gameQueues.get(gameQueue).size() + "|");
            }
            System.out.print("}");
        }


        public void sendserverPortNumber(int portNum){
            try{
                dataOut.writeInt(portNum);
                dataOut.flush();
            } catch (Exception e) {
                e.printStackTrace();
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
        //searchS.startMatchMakingThreads(); //NOT tickrate


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
