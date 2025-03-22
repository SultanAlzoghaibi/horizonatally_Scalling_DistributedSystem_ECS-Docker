import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;

import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

// ✅ Step 1: Confirm PlayerData Object is Created on Player2ST
// - Initialize PlayerData with user ID, username, and ELO rating
// - Print out all values to confirm correct creation

// ✅ Step 2: Confirm PlayerData is Sent to Server
// - Ensure sendPlayerData() actually sends the object using ObjectOutputStream
// - Print a success message after sending

// ✅ Step 3: Confirm PlayerData is Received by Server
// - Read the PlayerData object from ObjectInputStream on the server side
// - Print out received values to verify successful transmission

// ✅ Step 4: Store PlayerData in an Array (Matchmaking Queue)
// - Use an ArrayList to store PlayerData for matchmaking
// - Ensure new players are properly added to the queue

public class Player2ST extends Application {

    private static final int SHIFT_AMOUNT = 100;
    private int width = 200, height = 400;

    private TextArea message, textGridMessage, testText;
    private Button startb00;
    private Button startChess;
    private Button startCheckers;
    private Button startTictactoe;
    private Button startConnect4;

    private Button b01, b02, b03, b04, b05, b06, b07, b08, b09;

    private CscToSearchServer cscSS;
    private CscToGameServer cscGS;
    private int playerID;
    private int otherPlayerID;
    private int[] values;
    private char[][] server2dChar;
    private int maxTurns;
    private int turnsMade;
    private int myPoints;
    private int enemyPoints;
    private boolean buttonsEnabled;
    private boolean gameIsActive;
    private int gameServerPort;
    private Stage primaryStage;
    private String gameMode;

    PlayerData playerData;



    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        playerMenu(primaryStage);
    }
    public PlayerData createPlayerData() {

        Random rand = new Random();
        StringBuilder username = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            char randomChar = (char) ('A' + rand.nextInt(26)); // Random letter from A-Z
            username.append(randomChar);
        }



        PlayerData playerData = new PlayerData(
                rand.nextInt(100),
                username.toString(),
                "chess"
        );
        playerData.printPlayerData();
        return playerData;
    }


    public void playerMenu(Stage primaryStage) {
        // CHAT GTP TRANSLATED THIS FROM SWING TO JAVAFX
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.setTitle("The Game Menu");

        // Initialize arrays and variables
        values = new int[4];
        server2dChar = new char[3][3];
        turnsMade = 0;
        myPoints = 0;
        enemyPoints = 0;
        gameIsActive = false;
        buttonsEnabled = false;
        gameMode = "na";

        VBox menuLayout = new VBox();
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setSpacing(10);

        testText = new TextArea("lol");
        testText.setEditable(false);

        try {
            playerData = createPlayerData();
        }catch(Exception e) {
            System.out.println("error");
        }


        // "Start Game" button
        //startb00 = new Button("Start Matchmaking");
        startChess = new Button("Start Chess");
        startCheckers = new Button("Start Checkers");
        startTictactoe = new Button("Start Tictactoe");
        startConnect4 = new Button("Start Connect4");

        startChess.setOnAction(e -> {
            System.out.println("YOU PRESSED CHESS");
            playerData.setGameModeInterested("chess");
            matchmakingButtonPressed();
        });
        startCheckers.setOnAction(e -> {
            System.out.println("YOU PRESSED CHECKERS");
            playerData.setGameModeInterested("checkers");
            matchmakingButtonPressed();
        });

        startTictactoe.setOnAction(e -> {
            System.out.println("YOU PRESSED TICTACTOE");
            playerData.setGameModeInterested("tictactoe");
            matchmakingButtonPressed();
        });
        startConnect4.setOnAction(e -> {
            System.out.println("YOU PRESSED CONNECT4");
            playerData.setGameModeInterested("connect4");
            matchmakingButtonPressed();
        });

        menuLayout.getChildren().addAll(startChess, startCheckers, startTictactoe, startConnect4, testText);





        /*startb00.setOnAction(e -> {
            System.out.println("YOU PRESSED THE BUTTON");

            connectToSearchServer();   // Connect to the server
            hnadleB00Click();
            setUpLoadingScreen(primaryStage);
        });*/



        //menuLayout.getChildren().addAll(startChess, testText);

        Scene menuScene = new Scene(menuLayout, width, height);
        primaryStage.setScene(menuScene);

        // 🔹 Add dynamic window shifting on each run
        Random random = new Random();
        double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        int SHIFT_AMOUNT = 130; // Pixels to shift per instance
        int newX = (random.nextInt(5) * SHIFT_AMOUNT) % (int) (screenWidth - width);
        int newY = 100; // Fixed Y-position

        primaryStage.setX(newX);
        primaryStage.setY(newY);

        primaryStage.show();
        // end of CHAT GTP
    }
    public void matchmakingButtonPressed(){
        connectToSearchServer();   // Connect to the server
        hnadleB00Click();
        setUpLoadingScreen(primaryStage);
    }

    private void setUpLoadingScreen(Stage primaryStage) {
        gameIsActive = true;
        primaryStage.setTitle("MATCHMAKING LOADING...");

        // Main layout
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        // "MATCHMAKING LOADING..." Message
        Label loadingMessage = new Label("MATCHMAKING LOADING...");

        // Add elements to layout
        root.getChildren().add(loadingMessage);

        // Scene setup
        Scene loadingScene = new Scene(root, width, height);
        primaryStage.setScene(loadingScene);
        primaryStage.show();
    }

    private void setUpGameScene(Stage primaryStage) {
        // CHAT GTP TRANSLTED THIS FROM SWING TO JAVAFX
        gameIsActive = true;
        primaryStage.setTitle(gameMode + " for Player #" + playerID);
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);
        message = new TextArea("Turn-based game");
        message.setWrapText(true);
        message.setEditable(false);
        message.setMaxHeight(50);
        root.getChildren().add(message);
        textGridMessage = new TextArea();
        textGridMessage.setWrapText(true);
        textGridMessage.setEditable(false);
        textGridMessage.setMaxHeight(50);
        root.getChildren().add(textGridMessage);
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(10);
        buttonGrid.setVgap(10);
        buttonGrid.setAlignment(Pos.CENTER);
        b01 = new Button("1");
        b02 = new Button("2");
        b03 = new Button("3");
        b04 = new Button("4");
        b05 = new Button("5");
        b06 = new Button("6");
        b07 = new Button("7");
        b08 = new Button("8");
        b09 = new Button("9");
        buttonGrid.addRow(0, b01, b02, b03);
        buttonGrid.addRow(1, b04, b05, b06);
        buttonGrid.addRow(2, b07, b08, b09);
        setUpGameButtons();
        root.getChildren().add(buttonGrid);
        Scene gameScene = new Scene(root, width, height);
        primaryStage.setScene(gameScene);
        primaryStage.show();
        // END of CHAT GTP
        if (playerID == 1) {
            message.setText("You are player 1, you go first");
            otherPlayerID = 2;
            buttonsEnabled = true;
        } else {
            message.setText("You are player 2, wait for your turn");
            otherPlayerID = 1;
            buttonsEnabled = false;
            // Start waiting for the opponent in a separate thread
            Thread t = new Thread(this::updateTurn);
            t.start();
        }
        toggleButtons();
    }

    /**
     * Sets the onAction for each of the 9 game buttons.
     */
    private void setUpGameButtons() {
        b01.setOnAction(e -> handleButtonClick("1"));
        b02.setOnAction(e -> handleButtonClick("2"));
        b03.setOnAction(e -> handleButtonClick("3"));
        b04.setOnAction(e -> handleButtonClick("4"));
        b05.setOnAction(e -> handleButtonClick("5"));
        b06.setOnAction(e -> handleButtonClick("6"));
        b07.setOnAction(e -> handleButtonClick("7"));
        b08.setOnAction(e -> handleButtonClick("8"));
        b09.setOnAction(e -> handleButtonClick("9"));
    }

    /**
     * Called when a button (1-9) is clicked
     */
    private void hnadleB00Click(){
        if (cscSS != null) {
            playerData.printPlayerData();
            cscSS.sendPlayerData(playerData);
        }

    }

    private void handleButtonClick(String strBNum) {
        message.setText("You clicked button #" + strBNum + " now wait for next player's turn");
        textGridMessage.setText(server2dCharToString());

        turnsMade++;
        System.out.println("Turns made: " + turnsMade);

        buttonsEnabled = false;
        toggleButtons();

        // Send button to server
        if (cscGS != null) {
            cscGS.sendButtonNum(strBNum);
        }

        // If P2 hits max turns, check winner
        if (playerID == 2 && turnsMade == maxTurns) {
            System.out.println("win condition?");
            //checkWinner();
        } else {
            // Otherwise wait for the opponent

            new Thread(this::updateTurn).start();
        }
    }

    /**
     * Enables or disables the 9 game buttons based on 'buttonsEnabled'.
     */
    private void toggleButtons() {
        b01.setDisable(!buttonsEnabled);
        b02.setDisable(!buttonsEnabled);
        b03.setDisable(!buttonsEnabled);
        b04.setDisable(!buttonsEnabled);
        b05.setDisable(!buttonsEnabled);
        b06.setDisable(!buttonsEnabled);
        b07.setDisable(!buttonsEnabled);
        b08.setDisable(!buttonsEnabled);
        b09.setDisable(!buttonsEnabled);
    }

    /**
     * Connect to the server
     */


    public void connectToSearchServer() {
        cscSS = new CscToSearchServer();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown detected. Closing Search Server connection...");
            closeSearchServerConnection();
        }));
    }
    public void connectToGameServer() {
        cscGS = new CscToGameServer();
        System.out.println("Welcome to the game server");

        // 🔹 Add shutdown hook to close the connection when app stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown detected. Closing Game Server connection...");
            closeGameServerConnection();
        }));
    }
    private void closeGameServerConnection() {
        try {
            if (cscGS != null && cscGS.getSocket() != null && !cscGS.getSocket().isClosed()) {
                cscGS.getSocket().close();
                System.out.println("Game Server socket closed.");
            }
        } catch (IOException e) {
            System.out.println("Error closing Game Server socket: " + e.getMessage());
        }
    }
    private void closeSearchServerConnection() {
        try {
            if (cscSS != null && cscSS.getSocket() != null && !cscSS.getSocket().isClosed()) {
                cscSS.getSocket().close();
                System.out.println("Search Server socket closed.");
            }
        } catch (IOException e) {
            System.out.println("Error closing Search Server socket: " + e.getMessage());
        }
    }

    /**
     * Wait for the opponent's move
     */
    public void updateTurn() {

        String n = cscGS.receiveButtonNum();
        message.setText("Your opponent clicked #" + n + ", now your turn");
        textGridMessage.setText(server2dCharToString());

        // Print 2D array from the server
        for (char[] row : server2dChar) {
            System.out.println(Arrays.toString(row));
        }
        // If P1 hits max turns, check winner
        if (playerID == 1 && turnsMade == maxTurns) {
            System.out.println("win condition?");
            //checkWinner();
        } else {
            buttonsEnabled = true;
        }
        toggleButtons();
    }

    /**
     * Checks who won or if it's a tie
     */
    private void checkWinner() {
        buttonsEnabled = false;
        // Compare 'myPoints' vs 'enemyPoints' if you actually tracked them
        // Right now it's just a placeholder
        if (myPoints > enemyPoints) {
            message.setText("You won! You: " + myPoints + " points, Enemy: " + enemyPoints + " points");
        } else if (myPoints < enemyPoints) {
            message.setText("You lost! You: " + myPoints + " points, Enemy: " + enemyPoints + " points");
        } else {
            message.setText("It's a tie! Both have " + myPoints + " points.");
        }
        cscGS.closeConnection();
    }

    /**
     * Converts server2dChar to a human-readable string
     */
    public String server2dCharToString() {
        StringBuilder sb = new StringBuilder();
        for (char[] row : server2dChar) {
            sb.append("[");
            for (char cell : row) {
                sb.append("['").append(cell).append("'], ");
            }
            sb.setLength(sb.length() - 2); // remove trailing ", "
            sb.append("]\n");
        }
        return sb.toString();
    }

    /**
     * The networking client side
     */
    private class CscToGameServer {
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        public CscToGameServer() {
            System.out.println("Client side connection -- GameServerr");
            try {
                socket = new Socket("localhost", gameServerPort);

                dataOut = new DataOutputStream(socket.getOutputStream());
                dataIn = new DataInputStream(socket.getInputStream());

                playerID = dataIn.readInt();
                System.out.println("Player ID: " + playerID);

                // Values to send over the network
                maxTurns = dataIn.readInt() / 2;
                System.out.println("maxTurns = " + maxTurns);

                gameMode = dataIn.readUTF();
                System.out.println(gameMode);

                // Read the 3x3 char array from the server
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        server2dChar[i][j] = dataIn.readChar();
                    }
                }

            } catch (IOException e) {
                System.out.println("IO exception from CSC constructor");
            }
        }
        public void sendButtonNum(String strBNum) {
            try {
                // Send button as chars
                dataOut.writeChars(strBNum);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection sendButtonNum");
            }
        }
        public String receiveButtonNum() {
            String str = "N"; // placeholder
            try {
                // read one char for the button number

                str = String.valueOf(dataIn.readChar());
                System.out.println("player #" + otherPlayerID + " clicked button #" + str);

                // read updated board
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        server2dChar[i][j] = dataIn.readChar();
                    }
                }

            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection receiveButtonNum");
            }
            return str;
        }
        public void closeConnection() {
            try {
                socket.close();
                System.out.println("Closing connection");
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection closeConnection");
            }
        }

        public Socket getSocket() {
            return socket;
        }
    }


    private class CscToSearchServer{
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private ObjectOutputStream objectDataOut;
        private ObjectInputStream objectDataIn;

        public CscToSearchServer() {
            System.out.println("Client side connection -- SearchServer");
            try {
                socket = new Socket("localhost", 30000);

                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());

                objectDataOut = new ObjectOutputStream(dataOut);
                objectDataIn = new ObjectInputStream(dataIn);

                //playerID = dataIn.readInt();
                //System.out.println("Player ID: " + playerID);

                // Values to send over the network
                //maxTurns = dataIn.readInt() / 2;
                //System.out.println("maxTurns = " + maxTurns);

                waitForGameServerPortThread();


            } catch (IOException e) {
                System.out.println("IO exception from CSC constructor");
            }
        }

        public void sendPlayerData(PlayerData playerData) {
            try {
                objectDataOut.writeObject(playerData);  // Write entire object
                objectDataOut.flush();
                System.out.print("PlayerData sent to server: ");
                playerData.printPlayerData();
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection sendPlayerData: " + e.getMessage());
            } catch (NullPointerException e) {
                System.out.println("NullPointerException in ClientSideConnection sendPlayerData: " + e.getMessage());
            }
        }

        private void waitForGameServerPortThread() {
            Thread portReceiver = new Thread(() -> {
                try {
                    // Block and wait until the port number arrives:
                    gameServerPort = dataIn.readInt();
                    System.out.println("Received GameServer port: " + gameServerPort);
                    if(gameServerPort > 30000) {
                        closeConnection();
                        connectToGameServer();

                        // Chatgtp said u can so primary stage unless its with this
                        // Platform.runLater javaFX special thread
                        Platform.runLater(() -> {
                            if (primaryStage != null) {
                                setUpGameScene(primaryStage);
                            }
                        });
                    }

                } catch (IOException e) {
                    System.out.println("Error receiving port: " + e.getMessage());
                }
            });
            portReceiver.start();
        }


        public void closeConnection() {
            try {
                socket.close();
                System.out.println("Closing connection");
            } catch (IOException e) {
                System.out.println("IO exception in ClientSideConnection closeConnection");
            }
        }

        public Socket getSocket() {
            return socket;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}