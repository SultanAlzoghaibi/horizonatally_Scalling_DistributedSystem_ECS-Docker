package oneServerImplementation;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class PlayerT extends Application {

    private int width = 200, height = 600;

    private TextArea message, textGridMessage, testText;
    private Button startb00;
    private Button b01, b02, b03, b04, b05, b06, b07, b08, b09;

    private ClientSideConnection csc;
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

    @Override
    public void start(Stage primaryStage) {
        playerMenu(primaryStage);
    }

    public void playerMenu(Stage primaryStage) {
        // CHAT GTP TRANSLTED THIS FROM SWING TO JAVAFX
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


        VBox menuLayout = new VBox();
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setSpacing(10);

        testText = new TextArea("lol");
        testText.setEditable(false);

        // "Start Game" button
        startb00 = new Button("Start Matchmaking");
        startb00.setOnAction(e -> {
            System.out.println("YOU PRESSED THE BUTTON");
            gameIsActive = true;
            connectToServer();   // Connect to the server
            setUpGameScene(primaryStage);  // Build the actual game UI
        });

        menuLayout.getChildren().addAll(startb00, testText);

        Scene menuScene = new Scene(menuLayout, width, height);
        primaryStage.setScene(menuScene);
        primaryStage.show();
        // end of CHAT GTP

    }

    /**
     * Builds the main game UI scene and sets it on the primary stage.
     */
    private void setUpGameScene(Stage primaryStage) {
        // CHAT GTP TRANSLTED THIS FROM SWING TO JAVAFX
        gameIsActive = true;
        primaryStage.setTitle("The Game for Player #" + playerID);

        // Main layout for the game: VBox (Keeps elements stacked)
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        // Message area at the top
        message = new TextArea("Turn-based game");
        message.setWrapText(true);
        message.setEditable(false);
        message.setMaxHeight(50);
        root.getChildren().add(message);

        // Text Grid Message (for game updates)
        textGridMessage = new TextArea();
        textGridMessage.setWrapText(true);
        textGridMessage.setEditable(false);
        textGridMessage.setMaxHeight(50);
        root.getChildren().add(textGridMessage);

        // Bottom Panel for 3x3 Buttons
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(10);
        buttonGrid.setVgap(10);
        buttonGrid.setAlignment(Pos.CENTER);

        // Create 9 buttons
        b01 = new Button("1");
        b02 = new Button("2");
        b03 = new Button("3");
        b04 = new Button("4");
        b05 = new Button("5");
        b06 = new Button("6");
        b07 = new Button("7");
        b08 = new Button("8");
        b09 = new Button("9");

        // Add them to a 3x3 layout
        buttonGrid.addRow(0, b01, b02, b03);
        buttonGrid.addRow(1, b04, b05, b06);
        buttonGrid.addRow(2, b07, b08, b09);

        // Set event handlers for each button
        setUpGameButtons();

        root.getChildren().add(buttonGrid);

        // Scene setup
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
    private void handleButtonClick(String strBNum) {
        message.setText("You clicked button #" + strBNum + " now wait for next player's turn");
        textGridMessage.setText(server2dCharToString());
        turnsMade++;
        System.out.println("Turns made: " + turnsMade);
        buttonsEnabled = false;
        toggleButtons();
        // Send button to server
        if (csc != null) {
            csc.sendButtonNum(strBNum);
        }
        // If P2 hits max turns, check winner
        if (playerID == 2 && turnsMade == maxTurns) {
            checkWinner();
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
    public void connectToServer() {
        csc = new ClientSideConnection();
    }

    /**
     * Wait for the opponent's move
     */
    public void updateTurn() {
        String n = csc.receiveButtonNum();
        message.setText("Your opponent clicked #" + n + ", now your turn");
        textGridMessage.setText(server2dCharToString());

        // Print 2D array from the server
        for (char[] row : server2dChar) {
            System.out.println(Arrays.toString(row));
        }

        // If P1 hits max turns, check winner
        if (playerID == 1 && turnsMade == maxTurns) {
            checkWinner();
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
        csc.closeConnection();
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
    private class ClientSideConnection {
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;

        public ClientSideConnection() {
            System.out.println("Client side connection");
            try {
                socket = new Socket("3.234.246.29", 30001);
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());

                playerID = dataIn.readInt();
                System.out.println("Player ID: " + playerID);

                // Values to send over the network
                maxTurns = dataIn.readInt() / 2;
                System.out.println("maxTurns = " + maxTurns);

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
    }

    public static void main(String[] args) {
        // Launch the JavaFX app
        launch(args);
    }
}