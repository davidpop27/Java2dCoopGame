import java.io.*;
import java.net.*;

public class InstantNetworkManager {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Main game;

    public InstantNetworkManager(Main game) {
        this.game = game;
    }

    public void startServer(int port) {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                this.socket = serverSocket.accept();
                setupStreams();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void startClient(String ip, int port) {
        new Thread(() -> {
            try {
                this.socket = new Socket(ip, port);
                setupStreams();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupStreams() throws IOException {
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        new Thread(this::listen).start();
    }

    public void sendData(String message) {
        if (out != null) out.println(message);
    }

    private void listen() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                parse(msg);
            }
        } catch (IOException e) {
            System.out.println("Disconnected.");
        }
    }

    private void parse(String data) {

        if (data.equals("VIC")) {
            game.triggerVictoryState();
        } else if (data.equals("SECRET_VIC")) {
            game.triggerSecretVictoryState();
        } else if (data.equals("FAIL")) {
            game.triggerGameOverState();
        } else if (data.equals("RESET")) {
            game.resetGameLocal();
        }
        if (data.equals("VIC")) {
            game.triggerVictoryState();
        } else if (data.equals("SECRET_VIC")) {
            game.triggerSecretVictoryState();
        } else if (data.equals("FAIL")) {
            game.triggerGameOverState();
        } else if (data.startsWith("S:") || data.startsWith("C:")) {
            String[] p = data.substring(2).split(",");
            game.remotePlayer.gridX = Integer.parseInt(p[0]);
            game.remotePlayer.gridY = Integer.parseInt(p[1]);
            game.repaint();
        } else if (data.startsWith("B:")) {
            String[] p = data.substring(2).split(",");
            game.world.setBlock(Integer.parseInt(p[0]), Integer.parseInt(p[1]), 0);
            game.repaint();
        } else if (data.startsWith("P:")) {
            String[] p = data.substring(2).split(",");
            int targetX = Integer.parseInt(p[0]);
            int targetY = Integer.parseInt(p[1]);
            int materialType = Integer.parseInt(p[2]);


            game.world.setBlock(targetX, targetY, materialType);
            game.repaint();
        }
    }
}