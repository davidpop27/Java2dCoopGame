import java.awt.*;

public class GridPlayer {
    public int gridX, gridY;
    public float renderX, renderY;
    public final int size = 24;
    private final int offset = (GameWorld.BLOCK_SIZE - size) / 2;
    private int jumpArcRemaining = 0;

    public GridPlayer(int startGridX, int startGridY) {
        this.gridX = startGridX;
        this.gridY = startGridY;
        this.renderX = startGridX * GameWorld.BLOCK_SIZE + offset;
        this.renderY = startGridY * GameWorld.BLOCK_SIZE + offset;
    }

    public void move(int dx, GameWorld world) {
        int targetX = gridX + dx;
        if (targetX < 0 || targetX >= GameWorld.COLS) return;

        boolean pathBlocked = isTileBlocking(targetX, gridY, world);

        if (!pathBlocked) {
            gridX = targetX;
        } else {
            boolean spaceAboveBlocked = isTileBlocking(targetX, gridY - 1, world);
            boolean headSpaceBlocked = isTileBlocking(gridX, gridY - 1, world);

            if (!spaceAboveBlocked && !headSpaceBlocked) {
                gridX = targetX;
                gridY = gridY - 1;
            }
        }
    }

    private boolean isTileBlocking(int x, int y, GameWorld world) {
        int blockType = world.getBlock(x, y);
        if (blockType == 0 || blockType == 5 || blockType == 12) return false;
        if (blockType == 4 && y < 30) return false;

        if (y < 30) {
            if (this.gridY <= y) {
                return true;
            }
            return false;
        }

        return world.isSolid(x, y);
    }

    public void jump(GameWorld world) {
        boolean standingOnFloor = gridY + 1 >= GameWorld.ROWS || isTileBlocking(gridX, gridY + 1, world);
        if (standingOnFloor && jumpArcRemaining == 0) {
            jumpArcRemaining = 3;
        }
    }

    public void applyDiscretePhysics(GameWorld world) {
        if (jumpArcRemaining > 0) {
            if (gridY - 1 >= 0 && !isTileBlocking(gridX, gridY - 1, world)) {
                gridY--;
                jumpArcRemaining--;
            } else {
                jumpArcRemaining = 0;
            }
        } else if (gridY + 1 < GameWorld.ROWS && !isTileBlocking(gridX, gridY + 1, world)) {
            gridY++;
        }
    }

    public void updateVisualInterpolation() {
        float tx = gridX * GameWorld.BLOCK_SIZE + offset;
        float ty = gridY * GameWorld.BLOCK_SIZE + offset;
        renderX += (tx - renderX) * 0.25f;
        renderY += (ty - renderY) * 0.25f;
    }

    public boolean canReach(int tx, int ty, int maxCells) {
        int dx = tx - gridX;
        int dy = ty - gridY;
        return (dx * dx + dy * dy) <= (maxCells * maxCells);
    }

    public void draw(Graphics2D g2d, Color color, int camX, int camY) {
        g2d.setColor(color);
        int screenX = (int) renderX - camX;
        int screenY = (int) renderY - camY;
        g2d.fillRect(screenX, screenY, size, size);
    }
}