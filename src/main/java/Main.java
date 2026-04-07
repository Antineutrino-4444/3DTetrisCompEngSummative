import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3D Rotating Tetracube");
            GameState game = new GameState();
            GamePanel panel = new GamePanel(game);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.requestFocusInWindow();
            panel.start();
        });
    }

    static final class GamePanel extends JPanel {
        private final GameState game;
        private final Timer timer;

        GamePanel(GameState game) {
            this.game = game;
            this.timer = new Timer(450, e -> {
                game.tick();
                repaint();
            });

            setPreferredSize(new Dimension(980, 760));
            setBackground(new Color(12, 14, 18));
            setFocusable(true);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (game.gameOver) {
                        if (e.getKeyCode() == KeyEvent.VK_R) {
                            game.reset();
                            repaint();
                        }
                        return;
                    }

                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT -> game.moveHorizontal(-1);
                        case KeyEvent.VK_RIGHT -> game.moveHorizontal(1);
                        case KeyEvent.VK_DOWN -> game.softDrop();
                        case KeyEvent.VK_SPACE -> game.hardDrop();
                        case KeyEvent.VK_Z -> game.rotateInPlane(1);
                        case KeyEvent.VK_X -> game.rotateInPlane(-1);
                        case KeyEvent.VK_A -> game.rollDepth(1);
                        case KeyEvent.VK_S -> game.rollDepth(-1);
                        case KeyEvent.VK_V -> game.toggleXray();
                        case KeyEvent.VK_P -> game.paused = !game.paused;
                        default -> {
                            return;
                        }
                    }
                    repaint();
                }
            });
        }

        void start() {
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cell = 42;
            int boardX = 40;
            int boardY = 80;
            int viewW = GameState.WIDTH;
            int viewH = GameState.HEIGHT;

            g2.setColor(new Color(24, 28, 36));
            g2.fillRoundRect(boardX - 14, boardY - 14, viewW * cell + 28, viewH * cell + 28, 16, 16);

            char[][] projection = game.currentProjection();
            for (int y = 0; y < viewH; y++) {
                for (int x = 0; x < viewW; x++) {
                    int px = boardX + x * cell;
                    int py = boardY + y * cell;
                    g2.setColor(new Color(33, 38, 47));
                    g2.fillRect(px, py, cell - 1, cell - 1);

                    char c = projection[y][x];
                    if (c != '.') {
                        int depthBand = c - '0';
                        int shade = Math.max(70, 245 - depthBand * 20);
                        Color blockColor = new Color(70, shade, 255 - depthBand * 10);
                        g2.setColor(blockColor);
                        g2.fillRoundRect(px + 4, py + 4, cell - 9, cell - 9, 8, 8);
                    }
                }
            }

            g2.setColor(new Color(220, 230, 255));
            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            g2.drawString("3D Rotating Tetracube", 40, 34);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g2.drawString("Face: " + game.face + "   Gravity: " + game.gravity, 40, 60);
            g2.drawString("Score: " + game.score + "   Pieces: " + game.placedPieces, 420, 60);

            int sideX = 380;
            int sideY = 120;
            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g2.drawString("Layer Fill (current gravity)", sideX, sideY - 20);
            int layers = game.board.layerCount(game.gravity);
            int cap = game.board.layerCapacity(game.gravity);
            for (int i = 0; i < layers; i++) {
                int fill = game.board.layerFill(game.gravity, i);
                int barW = 220;
                int y = sideY + i * 22;
                g2.setColor(new Color(42, 48, 61));
                g2.fillRoundRect(sideX, y, barW, 14, 8, 8);
                int fw = (int) ((fill / (double) cap) * barW);
                g2.setColor(new Color(80, 190, 255));
                g2.fillRoundRect(sideX, y, fw, 14, 8, 8);
                g2.setColor(new Color(226, 234, 255));
                g2.drawString(String.format("L%02d  %2d/%2d", i, fill, cap), sideX + 230, y + 12);
            }

            g2.setColor(new Color(220, 230, 255));
            g2.drawString("Controls:", 40, 640);
            g2.drawString("←/→ move   ↓ soft drop   SPACE hard drop", 40, 664);
            g2.drawString("Z/X spin   A/S roll-depth   V x-ray   P pause", 40, 686);
            g2.drawString("R restart after game over", 40, 708);

            if (game.paused && !game.gameOver) {
                drawOverlay(g2, "Paused");
            }
            if (game.gameOver) {
                drawOverlay(g2, "Game Over - Press R to Restart");
            }

            if (game.showXray) {
                drawXray(g2, 700, 120);
            }
        }

        private void drawOverlay(Graphics2D g2, String message) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255));
            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            g2.drawString(message, 230, 380);
        }

        private void drawXray(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(20, 24, 32, 220));
            g2.fillRoundRect(x, y, 230, 560, 12, 12);
            g2.setColor(new Color(220, 230, 255));
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.drawString("X-RAY (Y slices)", x + 12, y + 20);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            for (int yy = GameState.HEIGHT - 1; yy >= 0; yy--) {
                int line = (GameState.HEIGHT - 1 - yy);
                int ty = y + 42 + line * 40;
                g2.setColor(new Color(198, 208, 240));
                g2.drawString(String.format("Y=%02d", yy), x + 8, ty);
                for (int z = 0; z < GameState.DEPTH; z++) {
                    int count = 0;
                    for (int xx = 0; xx < GameState.WIDTH; xx++) {
                        if (game.board.get(xx, yy, z)) count++;
                    }
                    if (count > 0) {
                        g2.setColor(new Color(90, 190, 255));
                        g2.fillRect(x + 60 + z * 24, ty - 12, 18, 14);
                        g2.setColor(new Color(8, 15, 28));
                        g2.drawString(Integer.toString(count), x + 65 + z * 24, ty);
                    } else {
                        g2.setColor(new Color(56, 66, 86));
                        g2.drawRect(x + 60 + z * 24, ty - 12, 18, 14);
                    }
                }
            }
        }
    }

    static final class GameState {
        static final int WIDTH = 6;
        static final int HEIGHT = 12;
        static final int DEPTH = 6;
        static final int ROTATION_CADENCE = 8;
        static final int ROTATION_WARNING = 2;

        final Board board = new Board(WIDTH, HEIGHT, DEPTH);
        final Random random = new Random();

        Gravity gravity = Gravity.NEG_Y;
        Face face = Face.FRONT;
        Piece active;
        int placedPieces;
        int score;
        boolean gameOver;
        boolean paused;
        boolean showXray;

        GameState() {
            spawnPiece();
        }

        void reset() {
            board.clear();
            gravity = Gravity.NEG_Y;
            face = Face.FRONT;
            placedPieces = 0;
            score = 0;
            gameOver = false;
            paused = false;
            showXray = false;
            spawnPiece();
        }

        void tick() {
            if (paused || gameOver) return;
            if (!tryMove(gravity.dx, gravity.dy, gravity.dz)) {
                lockActive();
            }
        }

        void toggleXray() {
            showXray = !showXray;
        }

        void moveHorizontal(int dir) {
            if (paused || gameOver) return;
            Vec3 step = face.screenRightWorld();
            if (dir < 0) step = step.negate();
            tryMove(step.x, step.y, step.z);
        }

        void softDrop() {
            if (paused || gameOver) return;
            if (!tryMove(gravity.dx, gravity.dy, gravity.dz)) {
                lockActive();
            }
        }

        void hardDrop() {
            if (paused || gameOver) return;
            while (tryMove(gravity.dx, gravity.dy, gravity.dz)) {
                // drop until collision
            }
            lockActive();
        }

        void rotateInPlane(int turns) {
            if (paused || gameOver) return;
            rotatePiece(face.screenForwardWorld().negate(), turns);
        }

        void rollDepth(int turns) {
            if (paused || gameOver) return;
            rotatePiece(face.viewDirectionWorld(), turns);
        }

        private void rotatePiece(Vec3 axis, int quarterTurns) {
            Piece rotated = active.rotated(axis, quarterTurns);
            int[][] kicks = {
                    {0, 0, 0},
                    {1, 0, 0}, {-1, 0, 0},
                    {0, 1, 0}, {0, -1, 0},
                    {0, 0, 1}, {0, 0, -1}
            };
            for (int[] k : kicks) {
                Piece candidate = rotated.shifted(k[0], k[1], k[2]);
                if (board.canPlace(candidate)) {
                    active = candidate;
                    return;
                }
            }
        }

        char[][] currentProjection() {
            char[][] view = new char[HEIGHT][WIDTH];
            for (char[] row : view) Arrays.fill(row, '.');

            for (int sy = 0; sy < HEIGHT; sy++) {
                for (int sx = 0; sx < WIDTH; sx++) {
                    List<Vec3> ray = face.rayForScreenCell(sx, sy, WIDTH, HEIGHT, DEPTH);
                    int depthHit = -1;
                    for (int i = 0; i < ray.size(); i++) {
                        Vec3 p = ray.get(i);
                        if (board.get(p.x, p.y, p.z) || (active != null && active.occupies(p.x, p.y, p.z))) {
                            depthHit = i;
                            break;
                        }
                    }
                    if (depthHit >= 0) {
                        view[sy][sx] = (char) ('0' + Math.min(9, (depthHit * 10) / Math.max(1, DEPTH)));
                    }
                }
            }
            return view;
        }

        private boolean tryMove(int dx, int dy, int dz) {
            Piece moved = active.shifted(dx, dy, dz);
            if (board.canPlace(moved)) {
                active = moved;
                return true;
            }
            return false;
        }

        private void spawnPiece() {
            Shape shape = Shape.BAG[random.nextInt(Shape.BAG.length)];
            List<Vec3> blocks = new ArrayList<>(shape.blocks);

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (Vec3 b : blocks) {
                minX = Math.min(minX, b.x);
                minY = Math.min(minY, b.y);
                minZ = Math.min(minZ, b.z);
                maxX = Math.max(maxX, b.x);
                maxY = Math.max(maxY, b.y);
                maxZ = Math.max(maxZ, b.z);
            }

            int spanX = maxX - minX + 1;
            int spanY = maxY - minY + 1;
            int spanZ = maxZ - minZ + 1;

            int ox = (WIDTH - spanX) / 2 - minX;
            int oy = (HEIGHT - spanY) / 2 - minY;
            int oz = (DEPTH - spanZ) / 2 - minZ;

            if (gravity.dx > 0) ox = -minX;
            if (gravity.dx < 0) ox = WIDTH - 1 - maxX;
            if (gravity.dy > 0) oy = -minY;
            if (gravity.dy < 0) oy = HEIGHT - 1 - maxY;
            if (gravity.dz > 0) oz = -minZ;
            if (gravity.dz < 0) oz = DEPTH - 1 - maxZ;

            active = new Piece(blocks, ox, oy, oz);
            int attempts = Math.max(WIDTH, Math.max(HEIGHT, DEPTH));
            for (int i = 0; i < attempts && !board.canPlace(active); i++) {
                active = active.shifted(-gravity.dx, -gravity.dy, -gravity.dz);
            }
            if (!board.canPlace(active)) {
                gameOver = true;
            }
        }

        private void lockActive() {
            board.place(active);
            placedPieces++;
            score += 10;

            int cleared = board.clearFullLayers(gravity);
            if (cleared > 0) {
                score += switch (cleared) {
                    case 1 -> 100;
                    case 2 -> 300;
                    case 3 -> 500;
                    default -> 800;
                };
            }

            int piecesUntilRotation = ROTATION_CADENCE - (placedPieces % ROTATION_CADENCE);
            if (piecesUntilRotation == ROTATION_CADENCE) {
                performRotation();
            }

            spawnPiece();
        }

        private void performRotation() {
            gravity = gravity.next();
            face = face.next();
            int chain = 0;
            while (true) {
                board.settle(gravity);
                int cleared = board.clearFullLayers(gravity);
                if (cleared == 0) break;
                chain++;
                score += 200 * chain * cleared;
            }
        }
    }

    enum Gravity {
        NEG_Y(0, -1, 0, "Down (-Y)"),
        POS_X(1, 0, 0, "Right (+X)"),
        POS_Y(0, 1, 0, "Up (+Y)"),
        NEG_X(-1, 0, 0, "Left (-X)");

        final int dx, dy, dz;
        final String label;

        Gravity(int dx, int dy, int dz, String label) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.label = label;
        }

        Gravity next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum Face {
        FRONT(new Vec3(1, 0, 0), new Vec3(0, -1, 0), new Vec3(0, 0, -1)),
        RIGHT(new Vec3(0, 0, -1), new Vec3(0, -1, 0), new Vec3(-1, 0, 0)),
        BACK(new Vec3(-1, 0, 0), new Vec3(0, -1, 0), new Vec3(0, 0, 1)),
        LEFT(new Vec3(0, 0, 1), new Vec3(0, -1, 0), new Vec3(1, 0, 0));

        final Vec3 right;
        final Vec3 down;
        final Vec3 viewDir;

        Face(Vec3 right, Vec3 down, Vec3 viewDir) {
            this.right = right;
            this.down = down;
            this.viewDir = viewDir;
        }

        Face next() {
            return values()[(ordinal() + 1) % values().length];
        }

        Vec3 screenRightWorld() {
            return right;
        }

        Vec3 screenForwardWorld() {
            return down;
        }

        Vec3 viewDirectionWorld() {
            return viewDir;
        }

        List<Vec3> rayForScreenCell(int sx, int sy, int width, int height, int depth) {
            List<Vec3> ray = new ArrayList<>(depth);
            for (int i = 0; i < depth; i++) {
                int x = switch (this) {
                    case FRONT -> sx;
                    case RIGHT -> width - 1 - i;
                    case BACK -> width - 1 - sx;
                    case LEFT -> i;
                };
                int y = height - 1 - sy;
                int z = switch (this) {
                    case FRONT -> depth - 1 - i;
                    case RIGHT -> depth - 1 - sx;
                    case BACK -> i;
                    case LEFT -> sx;
                };
                ray.add(new Vec3(x, y, z));
            }
            return ray;
        }
    }

    static final class Board {
        private final int w, h, d;
        private final boolean[][][] grid;

        Board(int w, int h, int d) {
            this.w = w;
            this.h = h;
            this.d = d;
            this.grid = new boolean[w][h][d];
        }

        void clear() {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    Arrays.fill(grid[x][y], false);
                }
            }
        }

        boolean canPlace(Piece p) {
            for (Vec3 b : p.worldBlocks()) {
                if (!inBounds(b.x, b.y, b.z) || grid[b.x][b.y][b.z]) return false;
            }
            return true;
        }

        void place(Piece p) {
            for (Vec3 b : p.worldBlocks()) {
                if (inBounds(b.x, b.y, b.z)) grid[b.x][b.y][b.z] = true;
            }
        }

        boolean get(int x, int y, int z) {
            return inBounds(x, y, z) && grid[x][y][z];
        }

        boolean inBounds(int x, int y, int z) {
            return x >= 0 && x < w && y >= 0 && y < h && z >= 0 && z < d;
        }

        int layerCount(Gravity g) {
            if (g.dx != 0) return w;
            if (g.dy != 0) return h;
            return d;
        }

        int layerCapacity(Gravity g) {
            if (g.dx != 0) return h * d;
            if (g.dy != 0) return w * d;
            return w * h;
        }

        int layerFill(Gravity g, int layer) {
            int count = 0;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) {
                        if ((g.dx != 0 && x == layer) || (g.dy != 0 && y == layer) || (g.dz != 0 && z == layer)) {
                            if (grid[x][y][z]) count++;
                        }
                    }
                }
            }
            return count;
        }

        int clearFullLayers(Gravity g) {
            int axisLen = layerCount(g);
            int cap = layerCapacity(g);
            boolean[] full = new boolean[axisLen];
            int cleared = 0;
            for (int i = 0; i < axisLen; i++) {
                if (layerFill(g, i) == cap) {
                    full[i] = true;
                    cleared++;
                }
            }
            if (cleared == 0) return 0;

            boolean[][][] next = new boolean[w][h][d];
            int write = gravityEndIndex(g);
            int step = gravityStep(g);
            for (int read = gravityEndIndex(g); read >= 0 && read < axisLen; read += step) {
                if (full[read]) continue;
                copyLayer(g, read, next, write);
                write += step;
            }
            copyInto(next);
            return cleared;
        }

        void settle(Gravity g) {
            if (g.dx != 0) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) settleLineX(y, z, g.dx > 0);
                }
            } else if (g.dy != 0) {
                for (int x = 0; x < w; x++) {
                    for (int z = 0; z < d; z++) settleLineY(x, z, g.dy > 0);
                }
            } else {
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) settleLineZ(x, y, g.dz > 0);
                }
            }
        }

        private void settleLineX(int y, int z, boolean towardPositive) {
            boolean[] line = new boolean[w];
            for (int x = 0; x < w; x++) line[x] = grid[x][y][z];
            for (int x = 0; x < w; x++) grid[x][y][z] = false;
            int write = towardPositive ? w - 1 : 0;
            int step = towardPositive ? -1 : 1;
            for (int read = towardPositive ? w - 1 : 0; read >= 0 && read < w; read += step) {
                if (line[read]) {
                    grid[write][y][z] = true;
                    write += step;
                }
            }
        }

        private void settleLineY(int x, int z, boolean towardPositive) {
            boolean[] line = new boolean[h];
            for (int y = 0; y < h; y++) line[y] = grid[x][y][z];
            for (int y = 0; y < h; y++) grid[x][y][z] = false;
            int write = towardPositive ? h - 1 : 0;
            int step = towardPositive ? -1 : 1;
            for (int read = towardPositive ? h - 1 : 0; read >= 0 && read < h; read += step) {
                if (line[read]) {
                    grid[x][write][z] = true;
                    write += step;
                }
            }
        }

        private void settleLineZ(int x, int y, boolean towardPositive) {
            boolean[] line = new boolean[d];
            for (int z = 0; z < d; z++) line[z] = grid[x][y][z];
            for (int z = 0; z < d; z++) grid[x][y][z] = false;
            int write = towardPositive ? d - 1 : 0;
            int step = towardPositive ? -1 : 1;
            for (int read = towardPositive ? d - 1 : 0; read >= 0 && read < d; read += step) {
                if (line[read]) {
                    grid[x][y][write] = true;
                    write += step;
                }
            }
        }

        private int gravityEndIndex(Gravity g) {
            if (g.dx > 0) return w - 1;
            if (g.dx < 0) return 0;
            if (g.dy > 0) return h - 1;
            if (g.dy < 0) return 0;
            if (g.dz > 0) return d - 1;
            return 0;
        }

        private int gravityStep(Gravity g) {
            return (g.dx > 0 || g.dy > 0 || g.dz > 0) ? -1 : 1;
        }

        private void copyLayer(Gravity g, int from, boolean[][][] dest, int to) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    for (int z = 0; z < d; z++) {
                        int sx = x, sy = y, sz = z;
                        int tx = x, ty = y, tz = z;
                        if (g.dx != 0) {
                            sx = from;
                            tx = to;
                        } else if (g.dy != 0) {
                            sy = from;
                            ty = to;
                        } else {
                            sz = from;
                            tz = to;
                        }
                        if (grid[sx][sy][sz]) dest[tx][ty][tz] = true;
                    }
                }
            }
        }

        private void copyInto(boolean[][][] source) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    System.arraycopy(source[x][y], 0, grid[x][y], 0, d);
                }
            }
        }
    }

    static final class Piece {
        private final List<Vec3> blocks;
        private final int ox, oy, oz;

        Piece(List<Vec3> blocks, int ox, int oy, int oz) {
            this.blocks = blocks;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
        }

        Piece shifted(int dx, int dy, int dz) {
            return new Piece(blocks, ox + dx, oy + dy, oz + dz);
        }

        Piece rotated(Vec3 axis, int quarterTurns) {
            int turns = Math.floorMod(quarterTurns, 4);
            List<Vec3> rotated = new ArrayList<>();
            for (Vec3 b : blocks) {
                Vec3 r = b;
                for (int i = 0; i < turns; i++) r = r.rotateQuarter(axis);
                rotated.add(r);
            }
            return new Piece(rotated, ox, oy, oz);
        }

        boolean occupies(int x, int y, int z) {
            for (Vec3 b : blocks) {
                if (ox + b.x == x && oy + b.y == y && oz + b.z == z) return true;
            }
            return false;
        }

        List<Vec3> worldBlocks() {
            List<Vec3> out = new ArrayList<>(blocks.size());
            for (Vec3 b : blocks) out.add(new Vec3(ox + b.x, oy + b.y, oz + b.z));
            return out;
        }
    }

    record Vec3(int x, int y, int z) {
        Vec3 negate() {
            return new Vec3(-x, -y, -z);
        }

        Vec3 rotateQuarter(Vec3 axis) {
            if (Math.abs(axis.x) == 1) return axis.x > 0 ? new Vec3(x, -z, y) : new Vec3(x, z, -y);
            if (Math.abs(axis.y) == 1) return axis.y > 0 ? new Vec3(z, y, -x) : new Vec3(-z, y, x);
            return axis.z > 0 ? new Vec3(-y, x, z) : new Vec3(y, -x, z);
        }
    }

    enum Shape {
        I(new Vec3[]{new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(2, 0, 0), new Vec3(3, 0, 0)}),
        L(new Vec3[]{new Vec3(0, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 2, 0), new Vec3(1, 0, 0)}),
        T(new Vec3[]{new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(2, 0, 0), new Vec3(1, 1, 0)}),
        O(new Vec3[]{new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(1, 1, 0)}),
        S(new Vec3[]{new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 1, 0), new Vec3(2, 1, 0)}),
        DEPTH_HOOK(new Vec3[]{new Vec3(0, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 1, 1), new Vec3(1, 1, 1)});

        static final Shape[] BAG = values();
        final List<Vec3> blocks;

        Shape(Vec3[] blocks) {
            this.blocks = List.of(blocks);
        }
    }
}
