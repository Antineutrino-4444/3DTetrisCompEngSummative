import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {
    private static final int WIDTH = 6;
    private static final int HEIGHT = 12;
    private static final int DEPTH = 6;
    private static final int ROTATION_CADENCE = 8;
    private static final int ROTATION_WARNING = 2;

    public static void main(String[] args) {
        new Game().run();
    }

    static final class Game {
        private final Board board = new Board(WIDTH, HEIGHT, DEPTH);
        private final Random random = new Random();
        private final Scanner scanner = new Scanner(System.in);
        private Gravity gravity = Gravity.NEG_Y;
        private Face face = Face.FRONT;
        private Piece active;
        private int placedPieces;
        private int score;
        private boolean gameOver;

        void run() {
            System.out.println("3D Rotating Tetracube (Console Prototype)");
            System.out.println("Commands: a/d move, s soft drop, w hard drop, q/e spin, z/c roll, x xray, p quit");
            spawnPiece();

            while (!gameOver) {
                int piecesUntilRotation = ROTATION_CADENCE - (placedPieces % ROTATION_CADENCE);
                if (piecesUntilRotation <= ROTATION_WARNING && piecesUntilRotation > 0) {
                    System.out.printf("Rotation incoming in %d piece(s). Next face: %s, next gravity: %s%n",
                            piecesUntilRotation, face.next(), gravity.next());
                }

                render(false);
                System.out.print("Command> ");
                String cmd = scanner.nextLine().trim().toLowerCase();
                if (cmd.isEmpty()) cmd = "s";
                if ("p".equals(cmd)) break;

                boolean lockedThisTurn = handleCommand(cmd);
                if (!lockedThisTurn) {
                    // gravity tick each turn to keep pace.
                    if (!tryMove(active, gravity.dx, gravity.dy, gravity.dz)) {
                        lockActive();
                    }
                }
            }

            System.out.printf("Game over. Score: %d | Pieces placed: %d%n", score, placedPieces);
        }

        private boolean handleCommand(String cmd) {
            return switch (cmd) {
                case "a" -> {
                    moveHorizontal(-1);
                    yield false;
                }
                case "d" -> {
                    moveHorizontal(1);
                    yield false;
                }
                case "s" -> {
                    if (!tryMove(active, gravity.dx, gravity.dy, gravity.dz)) {
                        lockActive();
                        yield true;
                    }
                    yield false;
                }
                case "w" -> {
                    while (tryMove(active, gravity.dx, gravity.dy, gravity.dz)) {
                        // keep dropping
                    }
                    lockActive();
                    yield true;
                }
                case "q" -> {
                    rotatePiece(screenAxisIntoWorld(ScreenAxis.IN_PLANE), 1);
                    yield false;
                }
                case "e" -> {
                    rotatePiece(screenAxisIntoWorld(ScreenAxis.IN_PLANE), -1);
                    yield false;
                }
                case "z" -> {
                    rotatePiece(screenAxisIntoWorld(ScreenAxis.DEPTH), 1);
                    yield false;
                }
                case "c" -> {
                    rotatePiece(screenAxisIntoWorld(ScreenAxis.DEPTH), -1);
                    yield false;
                }
                case "x" -> {
                    render(true);
                    yield false;
                }
                default -> {
                    System.out.println("Unknown command.");
                    yield false;
                }
            };
        }

        private void moveHorizontal(int dir) {
            Vec3 step = face.screenRightWorld();
            if (dir < 0) {
                step = step.negate();
            }
            tryMove(active, step.x, step.y, step.z);
        }

        private Vec3 screenAxisIntoWorld(ScreenAxis axis) {
            return switch (axis) {
                case IN_PLANE -> face.screenForwardWorld().negate();
                case DEPTH -> face.viewDirectionWorld();
            };
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

        private boolean tryMove(Piece piece, int dx, int dy, int dz) {
            Piece moved = piece.shifted(dx, dy, dz);
            if (board.canPlace(moved)) {
                active = moved;
                return true;
            }
            return false;
        }

        private void spawnPiece() {
            Shape shape = Shape.BAG[random.nextInt(Shape.BAG.length)];
            List<Vec3> blocks = new ArrayList<>();
            for (Vec3 v : shape.blocks) {
                blocks.add(v);
            }
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
            for (int i = 0; i < Math.max(WIDTH, Math.max(HEIGHT, DEPTH)) && !board.canPlace(active); i++) {
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
                System.out.printf("Cleared %d layer(s)!%n", cleared);
            }

            if (placedPieces % ROTATION_CADENCE == 0) {
                performRotation();
            }

            spawnPiece();
        }

        private void performRotation() {
            System.out.println("--- ROTATION EVENT ---");
            gravity = gravity.next();
            face = face.next();
            int chain = 0;
            while (true) {
                board.settle(gravity);
                int cleared = board.clearFullLayers(gravity);
                if (cleared == 0) break;
                chain++;
                score += (200 * chain * cleared);
                System.out.printf("Rotation chain %d: cleared %d layer(s)%n", chain, cleared);
            }
        }

        private void render(boolean xray) {
            System.out.printf("\nFace: %s | Gravity: %s | Score: %d | Pieces: %d%n",
                    face, gravity, score, placedPieces);

            char[][] view = new char[HEIGHT][WIDTH];
            for (char[] row : view) Arrays.fill(row, '.');

            for (int sy = 0; sy < HEIGHT; sy++) {
                for (int sx = 0; sx < WIDTH; sx++) {
                    List<Vec3> ray = face.rayForScreenCell(sx, sy, WIDTH, HEIGHT, DEPTH);
                    int depthHit = -1;
                    for (int i = 0; i < ray.size(); i++) {
                        Vec3 p = ray.get(i);
                        if (board.get(p.x, p.y, p.z) || active.occupies(p.x, p.y, p.z)) {
                            depthHit = i;
                            break;
                        }
                    }
                    if (depthHit >= 0) {
                        view[sy][sx] = depthChar(depthHit, DEPTH);
                    }
                }
            }

            for (char[] row : view) {
                System.out.println(new String(row));
            }

            System.out.println("Layer fill meters (perpendicular to gravity):");
            int layerCount = board.layerCount(gravity);
            for (int i = 0; i < layerCount; i++) {
                int fill = board.layerFill(gravity, i);
                int total = board.layerCapacity(gravity);
                System.out.printf("L%02d: %2d/%2d%n", i, fill, total);
            }

            if (xray) {
                System.out.println("X-ray slices:");
                board.printXray();
            }
        }

        private char depthChar(int depth, int max) {
            int band = Math.min(9, (depth * 10) / Math.max(1, max));
            return (char) ('0' + band);
        }
    }

    enum ScreenAxis { IN_PLANE, DEPTH }

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

        boolean canPlace(Piece p) {
            for (Vec3 b : p.worldBlocks()) {
                if (!inBounds(b.x, b.y, b.z) || grid[b.x][b.y][b.z]) return false;
            }
            return true;
        }

        void place(Piece p) {
            for (Vec3 b : p.worldBlocks()) {
                if (inBounds(b.x, b.y, b.z)) {
                    grid[b.x][b.y][b.z] = true;
                }
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
            boolean[] full = new boolean[axisLen];
            int cleared = 0;
            int capacity = layerCapacity(g);
            for (int i = 0; i < axisLen; i++) {
                if (layerFill(g, i) == capacity) {
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
                    for (int z = 0; z < d; z++) {
                        settleLineX(y, z, g.dx > 0);
                    }
                }
            } else if (g.dy != 0) {
                for (int x = 0; x < w; x++) {
                    for (int z = 0; z < d; z++) {
                        settleLineY(x, z, g.dy > 0);
                    }
                }
            } else {
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        settleLineZ(x, y, g.dz > 0);
                    }
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
            if (g.dx > 0 || g.dy > 0 || g.dz > 0) return -1;
            return 1;
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
                        if (grid[sx][sy][sz]) {
                            dest[tx][ty][tz] = true;
                        }
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

        void printXray() {
            for (int y = h - 1; y >= 0; y--) {
                System.out.printf("Y=%02d ", y);
                for (int z = 0; z < d; z++) {
                    int c = 0;
                    for (int x = 0; x < w; x++) {
                        if (grid[x][y][z]) c++;
                    }
                    System.out.print(c == 0 ? '.' : Integer.toString(c));
                }
                System.out.println();
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
                for (int i = 0; i < turns; i++) {
                    r = r.rotateQuarter(axis);
                }
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
            for (Vec3 b : blocks) {
                out.add(new Vec3(ox + b.x, oy + b.y, oz + b.z));
            }
            return out;
        }
    }

    record Vec3(int x, int y, int z) {
        Vec3 negate() {
            return new Vec3(-x, -y, -z);
        }

        Vec3 rotateQuarter(Vec3 axis) {
            if (Math.abs(axis.x) == 1) {
                return axis.x > 0 ? new Vec3(x, -z, y) : new Vec3(x, z, -y);
            }
            if (Math.abs(axis.y) == 1) {
                return axis.y > 0 ? new Vec3(z, y, -x) : new Vec3(-z, y, x);
            }
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
