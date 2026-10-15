import java.awt.*;
import java.util.Random;

public class GameWorld {
    public static final int BLOCK_SIZE = 32;
    public static final int COLS = 100;
    public static final int ROWS = 100;

    private final int[][] worldMap = new int[ROWS][COLS];
    private final int[][] blueprint = new int[ROWS][COLS];


    private final int[][] lavaPower = new int[ROWS][COLS];

    public GameWorld() {
        resetWorld();
    }

    public void resetWorld() {
        Random syncedRandom = new Random(421337);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                lavaPower[r][c] = 0;
                if (r < 30) {
                    worldMap[r][c] = 0;
                } else if (r == 30) {
                    if (c > 35 && c < 65) worldMap[r][c] = 6;
                    else worldMap[r][c] = 1;
                } else if (r < 35) {
                    worldMap[r][c] = (c > 35 && c < 65) ? 6 : 3;
                } else {
                    double rand = syncedRandom.nextDouble();
                    if (rand < 0.03) {
                        worldMap[r][c] = 12;
                        lavaPower[r][c] = 5;
                    } else if (rand < 0.10) worldMap[r][c] = 7;
                    else if (rand < 0.18) worldMap[r][c] = 8;
                    else worldMap[r][c] = 2;
                }
                blueprint[r][c] = 0;
            }
        }

        for (int c = 2; c < COLS - 2; c += 5) {
            if (c < 32 || c > 68) spawnTree(c);
        }

        worldMap[45][COLS - 2] = 11;

        int centerCol = COLS / 2;
        int targetRow = 29;

        for (int c = centerCol - 5; c <= centerCol + 5; c++) blueprint[targetRow][c] = 10;
        blueprint[targetRow - 1][centerCol - 5] = 10;
        blueprint[targetRow - 1][centerCol + 5] = 10;
        blueprint[targetRow - 2][centerCol - 5] = 10;
        blueprint[targetRow - 2][centerCol + 5] = 10;
        blueprint[targetRow - 3][centerCol - 5] = 10;
        blueprint[targetRow - 3][centerCol + 5] = 10;
        blueprint[targetRow - 4][centerCol - 5] = 10;
        blueprint[targetRow - 4][centerCol + 5] = 10;

        for (int r = targetRow - 4; r <= targetRow - 1; r++) {
            blueprint[r][centerCol - 1] = 10;
            blueprint[r][centerCol + 1] = 10;
        }

        blueprint[targetRow - 1][centerCol - 4] = 9;
        blueprint[targetRow - 1][centerCol + 4] = 9;
        blueprint[targetRow - 2][centerCol - 4] = 9;
        blueprint[targetRow - 2][centerCol + 4] = 9;
        blueprint[targetRow - 3][centerCol - 4] = 9;
        blueprint[targetRow - 3][centerCol + 4] = 9;
        blueprint[targetRow - 1][centerCol - 3] = 9;
        blueprint[targetRow - 1][centerCol + 3] = 9;
        blueprint[targetRow - 2][centerCol - 3] = 9;
        blueprint[targetRow - 2][centerCol + 3] = 9;
        blueprint[targetRow - 5][centerCol] = 9;
        blueprint[targetRow - 6][centerCol] = 9;

        for (int c = centerCol - 5; c <= centerCol + 5; c++) blueprint[targetRow - 4][c] = 4;
        blueprint[targetRow - 5][centerCol - 4] = 4;
        blueprint[targetRow - 5][centerCol + 4] = 4;
        blueprint[targetRow - 5][centerCol - 3] = 4;
        blueprint[targetRow - 5][centerCol + 3] = 4;
        blueprint[targetRow - 5][centerCol - 2] = 4;
        blueprint[targetRow - 5][centerCol + 2] = 4;
        blueprint[targetRow - 5][centerCol - 1] = 4;
        blueprint[targetRow - 5][centerCol + 1] = 4;

        blueprint[targetRow - 6][centerCol - 1] = 10;
        blueprint[targetRow - 6][centerCol + 1] = 10;
        blueprint[targetRow - 7][centerCol - 1] = 4;
        blueprint[targetRow - 7][centerCol] = 4;
        blueprint[targetRow - 7][centerCol + 1] = 4;
        blueprint[targetRow - 8][centerCol] = 4;
        blueprint[targetRow - 9][centerCol] = 10;
    }


    public void updateLavaFlow(Main game) {
        int[][] nextPower = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(lavaPower[r], 0, nextPower[r], 0, COLS);
        }

        for (int r = ROWS - 2; r >= 0; r--) {
            for (int c = 0; c < COLS; c++) {
                if (worldMap[r][c] == 12 && lavaPower[r][c] > 1) {
                    int currentP = lavaPower[r][c];


                    if (worldMap[r + 1][c] == 0 || worldMap[r + 1][c] == 5) {

                        if (nextPower[r + 1][c] < currentP) {
                            nextPower[r + 1][c] = currentP;
                            flowInto(c, r + 1, currentP, game);
                        }
                    } else {
                        int lateralPower = currentP - 1;
                        if (lateralPower > 0) {

                            if (c > 0 && (worldMap[r][c - 1] == 0 || worldMap[r][c - 1] == 5)) {
                                if (nextPower[r][c - 1] < lateralPower) {
                                    nextPower[r][c - 1] = lateralPower;
                                    flowInto(c - 1, r, lateralPower, game);
                                }
                            }

                            if (c < COLS - 1 && (worldMap[r][c + 1] == 0 || worldMap[r][c + 1] == 5)) {
                                if (nextPower[r][c + 1] < lateralPower) {
                                    nextPower[r][c + 1] = lateralPower;
                                    flowInto(c + 1, r, lateralPower, game);
                                }
                            }
                        }
                    }
                }
            }
        }


        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(nextPower[r], 0, lavaPower[r], 0, COLS);
        }
    }

    private void flowInto(int x, int y, int power, Main game) {
        worldMap[y][x] = 12;
        lavaPower[y][x] = power;
        if (game.isMultiplayerActive()) {
            game.getNetworkManager().sendData("P:" + x + "," + y + ",12");
        }
    }

    private void spawnTree(int baseCol) {
        int surfaceRow = 30;
        for (int i = 1; i <= 4; i++) worldMap[surfaceRow - i][baseCol] = 4;
        worldMap[surfaceRow - 5][baseCol] = 5;
        worldMap[surfaceRow - 5][baseCol - 1] = 5;
        worldMap[surfaceRow - 5][baseCol + 1] = 5;
        worldMap[surfaceRow - 6][baseCol] = 5;
    }

    public int getBlock(int x, int y) {
        if (x >= 0 && x < COLS && y >= 0 && y < ROWS) return worldMap[y][x];
        return 0;
    }


    public void setBlock(int x, int y, int type) {
        if (x >= 0 && x < COLS && y >= 0 && y < ROWS) {
            worldMap[y][x] = type;
            if (type == 0) {
                lavaPower[y][x] = 0;
            }
        }
    }

    public int getBlueprintBlock(int x, int y) {
        if (x >= 0 && x < COLS && y >= 0 && y < ROWS) return blueprint[y][x];
        return 0;
    }

    public boolean isSolid(int x, int y) {
        int b = getBlock(x, y);
        return b != 0 && b != 5 && b != 12;
    }

    public boolean isMonumentComplete() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (blueprint[r][c] != 0 && worldMap[r][c] != blueprint[r][c]) return false;
            }
        }
        return true;
    }

    public void draw(Graphics2D g2d, int hoveredX, int hoveredY, boolean canReach, int camX, int camY) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int type = worldMap[r][c];
                int bpType = blueprint[r][c];
                int screenX = c * BLOCK_SIZE - camX;
                int screenY = r * BLOCK_SIZE - camY;

                if (screenX < -BLOCK_SIZE || screenX > 800 || screenY < -BLOCK_SIZE || screenY > 600) continue;

                if (type == 0 && bpType != 0) {
                    String letterIndicator = "";
                    if (bpType == 4) {
                        g2d.setColor(new Color(139, 69, 19, 50));
                        letterIndicator = "L";
                    } else if (bpType == 9) {
                        g2d.setColor(new Color(0, 230, 255, 50));
                        letterIndicator = "S";
                    } else if (bpType == 10) {
                        g2d.setColor(new Color(160, 160, 160, 70));
                        letterIndicator = "F";
                    }
                    g2d.fillRect(screenX + 2, screenY + 2, BLOCK_SIZE - 4, BLOCK_SIZE - 4);
                    g2d.setColor(new Color(0, 191, 255, 120));
                    g2d.drawRect(screenX + 2, screenY + 2, BLOCK_SIZE - 4, BLOCK_SIZE - 4);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.setColor(new Color(255, 255, 255, 190));
                    g2d.drawString(letterIndicator, screenX + 11, screenY + 20);
                }

                if (type == 1) {
                    g2d.setColor(new Color(34, 139, 34));
                    g2d.fillRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);
                } else if (type == 2) {
                    g2d.setColor(Color.GRAY);
                    g2d.fillRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);
                } else if (type == 3) {
                    g2d.setColor(new Color(101, 67, 33));
                    g2d.fillRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);
                } else if (type == 4) {
                    g2d.setColor(new Color(139, 69, 19));
                    g2d.fillRect(screenX + 6, screenY, BLOCK_SIZE - 12, BLOCK_SIZE);
                } else if (type == 5) {
                    g2d.setColor(new Color(0, 100, 0, 220));
                    g2d.fillRect(screenX + 2, screenY + 2, BLOCK_SIZE - 4, BLOCK_SIZE - 4);
                } else if (type == 6) {
                    g2d.setColor(new Color(238, 214, 175));
                    g2d.fillRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);
                } else if (type == 7) {
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.fillRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);
                    g2d.setColor(new Color(210, 180, 140));
                    g2d.fillRect(screenX + 8, screenY + 8, 10, 10);
                } else if (type == 8) {
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.fillRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(screenX + 6, screenY + 10, 8, 8);
                } else if (type == 9) {
                    g2d.setColor(new Color(240, 248, 255, 160));
                    g2d.fillRect(screenX + 2, screenY + 2, BLOCK_SIZE - 4, BLOCK_SIZE - 4);
                    g2d.setColor(Color.WHITE);
                    g2d.drawLine(screenX + 6, screenY + 6, screenX + 14, screenY + 14);
                } else if (type == 10) {
                    g2d.setColor(new Color(192, 192, 192));
                    g2d.fillRect(screenX + 1, screenY + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2);
                } else if (type == 11) {
                    g2d.setColor(new Color(0, 128, 0));
                    g2d.fillRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);
                    g2d.setColor(new Color(0, 255, 128));
                    g2d.fillRect(screenX + 6, screenY + 6, BLOCK_SIZE - 12, BLOCK_SIZE - 12);
                } else if (type == 12) {
                    g2d.setColor(new Color(255, 69, 0));
                    g2d.fillRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);
                    g2d.setColor(new Color(255, 140, 0));
                    g2d.fillRect(screenX + 2, screenY + 4, BLOCK_SIZE - 4, BLOCK_SIZE - 6);
                }

                g2d.setColor(new Color(255, 255, 255, 12));
                g2d.drawRect(screenX, screenY, BLOCK_SIZE, BLOCK_SIZE);

                if (canReach && r == hoveredY && c == hoveredX) {
                    g2d.setColor(new Color(255, 255, 0, 130));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRect(screenX + 1, screenY + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2);
                    g2d.setStroke(new BasicStroke(1));
                }
            }
        }
    }
}