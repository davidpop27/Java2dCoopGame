import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Main extends JPanel implements Runnable {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    public enum State {MENU, GAME, VICTORY, SECRET_VICTORY, GAMEOVER}

    private State currentState = State.MENU;

    public final GameWorld world = new GameWorld();

    public final GridPlayer localPlayer = new GridPlayer(46, 28);
    public final GridPlayer remotePlayer = new GridPlayer(54, 28);
    private final InstantNetworkManager networkManager = new InstantNetworkManager(this);

    private boolean multiplayerActive = false;
    private boolean serverSide = false;
    private int mouseX = 0, mouseY = 0;


    private int invGrass = 0, invDirt = 0, invStone = 0;
    private int invWood = 0, invSand = 0, invIronOre = 0, invCoal = 0;
    private int invGlass = 0, invIronBars = 0;
    private int activePlacementMaterial = 4;

    private int timeRemainingSeconds = 1800;
    private int secondTickCounter = 0;

    private JButton quitButton, hostButton, joinButton, furnaceSmeltGlassBtn, furnaceSmeltIronBtn, resetButton;
    private JRadioButton selectWoodRb, selectGlassRb, selectIronRb, selectGrassRb, selectDirtRb, selectStoneRb;
    private JTextField ipInputField;
    private JLabel ipLabel;

    private Thread gameThread = null;
    private boolean running = false;
    private int gravityTickCounter = 0;

    private int camX = 0;
    private int camY = 0;

    public Main() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(new Color(135, 206, 235));
        this.setLayout(null);

        initMenuUI();
        setupKeyBindings();

        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentState != State.GAME) return;

                int gridX = (e.getX() + camX) / GameWorld.BLOCK_SIZE;
                int gridY = (e.getY() + camY) / GameWorld.BLOCK_SIZE;

                if (localPlayer.canReach(gridX, gridY, 5)) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        int targeted = world.getBlock(gridX, gridY);

                        if (targeted != 0 && targeted != 5 && targeted != 12) {

                            if (targeted == 11) {
                                currentState = State.SECRET_VICTORY;
                                world.setBlock(gridX, gridY, 0);
                                if (multiplayerActive) {
                                    networkManager.sendData("B:" + gridX + "," + gridY);
                                    networkManager.sendData("SECRET_VIC");
                                }
                                triggerSecretVictoryState();
                                return;
                            }

                            if (targeted == 1) invGrass++;
                            else if (targeted == 2) invStone++;
                            else if (targeted == 3) invDirt++;
                            else if (targeted == 4) invWood++;
                            else if (targeted == 6) invSand++;
                            else if (targeted == 7) invIronOre++;
                            else if (targeted == 8) invCoal++;
                            else if (targeted == 9) invGlass++;
                            else if (targeted == 10) invIronBars++;

                            world.setBlock(gridX, gridY, 0);
                            if (multiplayerActive) networkManager.sendData("B:" + gridX + "," + gridY);
                        }
                    } else if (SwingUtilities.isRightMouseButton(e) && world.getBlock(gridX, gridY) == 0) {
                        int requiredType = world.getBlueprintBlock(gridX, gridY);
                        boolean placed = false;

                        if (requiredType != 0) {
                            if (activePlacementMaterial == requiredType) {
                                if (activePlacementMaterial == 4 && invWood > 0) {
                                    world.setBlock(gridX, gridY, 4);
                                    invWood--;
                                    placed = true;
                                } else if (activePlacementMaterial == 9 && invGlass > 0) {
                                    world.setBlock(gridX, gridY, 9);
                                    invGlass--;
                                    placed = true;
                                } else if (activePlacementMaterial == 10 && invIronBars > 0) {
                                    world.setBlock(gridX, gridY, 10);
                                    invIronBars--;
                                    placed = true;
                                }
                            }
                        } else {
                            if (activePlacementMaterial == 1 && invGrass > 0) {
                                world.setBlock(gridX, gridY, 1);
                                invGrass--;
                                placed = true;
                            } else if (activePlacementMaterial == 3 && invDirt > 0) {
                                world.setBlock(gridX, gridY, 3);
                                invDirt--;
                                placed = true;
                            } else if (activePlacementMaterial == 2 && invStone > 0) {
                                world.setBlock(gridX, gridY, 2);
                                invStone--;
                                placed = true;
                            }
                        }

                        if (placed && multiplayerActive) {
                            networkManager.sendData("P:" + gridX + "," + gridY + "," + activePlacementMaterial);
                        }
                    }
                }
            }
        });
    }

    public boolean isServerSide() {
        return serverSide;
    }

    public InstantNetworkManager getNetworkManager() {
        return networkManager;
    }

    public boolean isMultiplayerActive() {
        return multiplayerActive;
    }

    public void triggerGameOverState() {
        this.currentState = State.GAMEOVER;
        toggleHudVisibility(false);
        resetButton.setVisible(true);
        repaint();
    }

    public void triggerVictoryState() {
        this.currentState = State.VICTORY;
        toggleHudVisibility(false);
        resetButton.setVisible(true);
        repaint();
    }

    public void triggerSecretVictoryState() {
        this.currentState = State.SECRET_VICTORY;
        toggleHudVisibility(false);
        resetButton.setVisible(true);
        repaint();
    }

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();
        im.put(KeyStroke.getKeyStroke("pressed A"), "moveLeft");
        im.put(KeyStroke.getKeyStroke("pressed D"), "moveRight");
        im.put(KeyStroke.getKeyStroke("pressed W"), "jump");

        am.put("moveLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentState == State.GAME) {
                    localPlayer.move(-1, world);
                    broadcastPosition();
                }
            }
        });
        am.put("moveRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentState == State.GAME) {
                    localPlayer.move(1, world);
                    broadcastPosition();
                }
            }
        });
        am.put("jump", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentState == State.GAME) {
                    localPlayer.jump(world);
                    broadcastPosition();
                }
            }
        });
    }

    private void broadcastPosition() {
        if (multiplayerActive) {
            String prefix = serverSide ? "S:" : "C:";
            networkManager.sendData(prefix + localPlayer.gridX + "," + localPlayer.gridY);
        }
    }

    private void initMenuUI() {
        quitButton = new JButton("X");
        quitButton.setBounds(WIDTH - 60, 10, 50, 40);
        quitButton.setFont(new Font("Arial", Font.BOLD, 16));
        quitButton.setBackground(new Color(220, 53, 69));
        quitButton.setForeground(Color.WHITE);
        quitButton.setBorderPainted(false);
        quitButton.addActionListener(e -> System.exit(0));
        this.add(quitButton);


        resetButton = new JButton("Incearca din nou");
        resetButton.setBounds(WIDTH / 2 - 100, HEIGHT / 2 + 80, 200, 50);
        resetButton.setFont(new Font("Arial", Font.BOLD, 16));
        resetButton.setBackground(new Color(40, 167, 69));
        resetButton.setForeground(Color.WHITE);
        resetButton.setVisible(false);
        resetButton.addActionListener(e -> {
            if (multiplayerActive) {
                networkManager.sendData("RESET");
            }
            resetGameLocal();
        });
        this.add(resetButton);

        hostButton = new JButton("Joaca Host");
        hostButton.setBounds(WIDTH / 2 - 100, HEIGHT / 2 - 60, 200, 50);
        hostButton.setFont(new Font("Arial", Font.PLAIN, 18));
        hostButton.addActionListener(e -> {
            serverSide = true;
            networkManager.startServer(8888);
            startGame();
        });
        this.add(hostButton);

        ipLabel = new JLabel("Adresa IP:");
        ipLabel.setBounds(WIDTH / 2 - 100, HEIGHT / 2 + 10, 200, 20);
        this.add(ipLabel);

        ipInputField = new JTextField("xxx.x.x.x");
        ipInputField.setBounds(WIDTH / 2 - 100, HEIGHT / 2 + 35, 200, 30);
        this.add(ipInputField);

        joinButton = new JButton("Alatura-te");
        joinButton.setBounds(WIDTH / 2 - 100, HEIGHT / 2 + 80, 200, 50);
        joinButton.setFont(new Font("Arial", Font.PLAIN, 18));
        joinButton.addActionListener(e -> {
            serverSide = false;
            networkManager.startClient(ipInputField.getText(), 8888);
            startGame();
        });
        this.add(joinButton);

        furnaceSmeltGlassBtn = new JButton("Geam (1 Nisip + 1 Carbune)");
        furnaceSmeltGlassBtn.setBounds(15, 180, 210, 25);
        furnaceSmeltGlassBtn.addActionListener(e -> {
            if (invSand > 0 && invCoal > 0) {
                invSand--;
                invCoal--;
                invGlass++;
            }
        });
        this.add(furnaceSmeltGlassBtn);

        furnaceSmeltIronBtn = new JButton("Fier (1 Min. Fier + 1 Carbune)");
        furnaceSmeltIronBtn.setBounds(15, 210, 210, 25);
        furnaceSmeltIronBtn.addActionListener(e -> {
            if (invIronOre > 0 && invCoal > 0) {
                invIronOre--;
                invCoal--;
                invIronBars++;
            }
        });
        this.add(furnaceSmeltIronBtn);

        selectGrassRb = new JRadioButton("Iarba");
        selectGrassRb.setBounds(15, 245, 150, 20);
        selectDirtRb = new JRadioButton("Pamant");
        selectDirtRb.setBounds(15, 265, 150, 20);
        selectStoneRb = new JRadioButton("Piatra");
        selectStoneRb.setBounds(15, 285, 150, 20);
        selectWoodRb = new JRadioButton("Lemn", true);
        selectWoodRb.setBounds(15, 305, 150, 20);
        selectGlassRb = new JRadioButton("Geam");
        selectGlassRb.setBounds(15, 325, 150, 20);
        selectIronRb = new JRadioButton("Fier");
        selectIronRb.setBounds(15, 345, 150, 20);

        ButtonGroup bg = new ButtonGroup();
        bg.add(selectGrassRb);
        bg.add(selectDirtRb);
        bg.add(selectStoneRb);
        bg.add(selectWoodRb);
        bg.add(selectGlassRb);
        bg.add(selectIronRb);

        selectGrassRb.addActionListener(e -> activePlacementMaterial = 1);
        selectDirtRb.addActionListener(e -> activePlacementMaterial = 3);
        selectStoneRb.addActionListener(e -> activePlacementMaterial = 2);
        selectWoodRb.addActionListener(e -> activePlacementMaterial = 4);
        selectGlassRb.addActionListener(e -> activePlacementMaterial = 9);
        selectIronRb.addActionListener(e -> activePlacementMaterial = 10);

        this.add(selectGrassRb);
        this.add(selectDirtRb);
        this.add(selectStoneRb);
        this.add(selectWoodRb);
        this.add(selectGlassRb);
        this.add(selectIronRb);
        toggleHudVisibility(false);
    }

    private void toggleHudVisibility(boolean visible) {
        furnaceSmeltGlassBtn.setVisible(visible);
        furnaceSmeltIronBtn.setVisible(visible);
        selectGrassRb.setVisible(visible);
        selectDirtRb.setVisible(visible);
        selectStoneRb.setVisible(visible);
        selectWoodRb.setVisible(visible);
        selectGlassRb.setVisible(visible);
        selectIronRb.setVisible(visible);
    }


    public void resetGameLocal() {
        resetButton.setVisible(false);
        toggleHudVisibility(true);


        invGrass = 0;
        invDirt = 0;
        invStone = 0;
        invWood = 0;
        invSand = 0;
        invIronOre = 0;
        invCoal = 0;
        invGlass = 0;
        invIronBars = 0;
        activePlacementMaterial = 4;
        selectWoodRb.setSelected(true);


        localPlayer.gridX = 46;
        localPlayer.gridY = 28;
        localPlayer.renderX = 46 * GameWorld.BLOCK_SIZE + ((GameWorld.BLOCK_SIZE - localPlayer.size) / 2);
        localPlayer.renderY = 28 * GameWorld.BLOCK_SIZE + ((GameWorld.BLOCK_SIZE - localPlayer.size) / 2);

        remotePlayer.gridX = 54;
        remotePlayer.gridY = 28;
        remotePlayer.renderX = 54 * GameWorld.BLOCK_SIZE + ((GameWorld.BLOCK_SIZE - remotePlayer.size) / 2);
        remotePlayer.renderY = 28 * GameWorld.BLOCK_SIZE + ((GameWorld.BLOCK_SIZE - remotePlayer.size) / 2);

        world.resetWorld();
        timeRemainingSeconds = 1800;
        secondTickCounter = 0;
        gravityTickCounter = 0;
        currentState = State.GAME;
        this.requestFocusInWindow();
        repaint();
    }

    private void startGame() {
        hostButton.setVisible(false);
        joinButton.setVisible(false);
        ipInputField.setVisible(false);
        ipLabel.setVisible(false);
        resetGameLocal();
        multiplayerActive = true;

        if (gameThread == null) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        FontMetrics fm;

        if (currentState == State.MENU) {
            g2d.setFont(new Font("Impact", Font.PLAIN, 48));
            g2d.setColor(new Color(34, 139, 34));
            String text = "COLABORAREA";
            fm = g2d.getFontMetrics();
            g2d.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, HEIGHT / 2 - 120);
        } else if (currentState == State.VICTORY) {
            g2d.setFont(new Font("Impact", Font.PLAIN, 48));
            g2d.setColor(Color.GREEN);
            String text = "BRAVO BAIETI! ATI TERMINAT MONUMENTUL";
            fm = g2d.getFontMetrics();
            g2d.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, HEIGHT / 2 - 50);
        } else if (currentState == State.SECRET_VICTORY) {
            g2d.setColor(new Color(0, 70, 30));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setFont(new Font("Impact", Font.PLAIN, 42));
            g2d.setColor(new Color(0, 255, 128));
            fm = g2d.getFontMetrics();

            String t1 = "SFARSITUL SECRET!";
            String t2 = "DE MAINE NU MAI MUNCIM!";
            g2d.drawString(t1, WIDTH / 2 - fm.stringWidth(t1) / 2, HEIGHT / 2 - 50);
            g2d.drawString(t2, WIDTH / 2 - fm.stringWidth(t2) / 2, HEIGHT / 2 + 20);
        } else if (currentState == State.GAMEOVER) {

            g2d.setFont(new Font("Impact", Font.PLAIN, 48));
            g2d.setColor(Color.RED);
            String text = "GAME OVER, AIUREA!";
            fm = g2d.getFontMetrics();
            g2d.drawString(text, WIDTH / 2 - fm.stringWidth(text) / 2, HEIGHT / 2 - 50);
        } else {
            camX = (int) localPlayer.renderX - (WIDTH / 2);
            camY = (int) localPlayer.renderY - (HEIGHT / 2);
            camX = Math.max(0, Math.min(camX, GameWorld.COLS * GameWorld.BLOCK_SIZE - WIDTH));
            camY = Math.max(0, Math.min(camY, GameWorld.ROWS * GameWorld.BLOCK_SIZE - HEIGHT));

            int gridX = (mouseX + camX) / GameWorld.BLOCK_SIZE;
            int gridY = (mouseY + camY) / GameWorld.BLOCK_SIZE;

            world.draw(g2d, gridX, gridY, localPlayer.canReach(gridX, gridY, 5), camX, camY);
            localPlayer.draw(g2d, Color.BLUE, camX, camY);
            remotePlayer.draw(g2d, Color.RED, camX, camY);

            g2d.setColor(new Color(0, 0, 0, 165));
            g2d.fillRect(10, 10, 240, 160);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("RESURSE AJUTATOARE:", 15, 23);
            g2d.drawString("Iarba: " + invGrass + " | Pamant: " + invDirt + " | Piatra: " + invStone, 15, 38);

            g2d.drawString("NEPRELUCRATE:", 15, 60);
            g2d.drawString("Lemn: " + invWood + " | Nisip: " + invSand, 15, 75);
            g2d.drawString("Min. Fier: " + invIronOre + " | Carbune: " + invCoal, 15, 90);
            g2d.drawString("FINITE:", 15, 112);
            g2d.drawString("Geam: " + invGlass + " | Fier: " + invIronBars, 15, 127);

            g2d.setColor(Color.YELLOW);
            String materialName = "Lemn";
            if (activePlacementMaterial == 1) materialName = "Iarba";
            if (activePlacementMaterial == 3) materialName = "Pamant";
            if (activePlacementMaterial == 2) materialName = "Piatra";
            if (activePlacementMaterial == 9) materialName = "Geam";
            if (activePlacementMaterial == 10) materialName = "Fier";
            g2d.drawString("IN MANA: " + materialName, 15, 152);

            int minutes = timeRemainingSeconds / 60;
            int seconds = timeRemainingSeconds % 60;
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(WIDTH / 2 - 70, 10, 140, 30);
            g2d.setColor(Color.ORANGE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(String.format("TIMP RAMAS: %02d:%02d", minutes, seconds), WIDTH / 2 - 60, 31);
        }
    }

    @Override
    public void run() {
        while (running) {
            if (currentState == State.GAME) {
                gravityTickCounter++;

                if (gravityTickCounter % 10 == 0) {
                    localPlayer.applyDiscretePhysics(world);
                    if (world.getBlock(localPlayer.gridX, localPlayer.gridY) == 12) {
                        triggerGameOverState();
                        if (multiplayerActive) networkManager.sendData("FAIL");
                    }
                    broadcastPosition();
                }

                if (serverSide && gravityTickCounter % 20 == 0) {
                    world.updateLavaFlow(this);
                }

                secondTickCounter++;
                if (secondTickCounter >= 62) {
                    secondTickCounter = 0;
                    if (timeRemainingSeconds > 0) timeRemainingSeconds--;
                    else {
                        triggerGameOverState();
                        if (multiplayerActive) networkManager.sendData("FAIL");
                    }
                }

                if (world.isMonumentComplete()) {
                    triggerVictoryState();
                    if (multiplayerActive) networkManager.sendData("VIC");
                }
            }
            localPlayer.updateVisualInterpolation();
            remotePlayer.updateVisualInterpolation();
            repaint();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Colaborarea");
            Main gamePanel = new Main();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(gamePanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}