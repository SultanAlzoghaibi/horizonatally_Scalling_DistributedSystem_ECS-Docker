import java.io.Serializable;

public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String username;
    private int elo;


    public PlayerData(int userId, String username, int elo) {
        this.userId = userId;
        this.username = username;
        this.elo = elo;
    }
    public int getUserId() {return userId;}

    public String getUsername() {return username;}

    public int getElo() {return elo;}

    public void setElo(int elo) { this.elo = elo; }

    public void setUsername(String username) {this.username = username;}

    public void setUserId(int userId) {this.userId = userId;}

    public void printPlayerData() {
        System.out.println("Username: " + username);
        System.out.println("ELO: " + elo);
    }
}

