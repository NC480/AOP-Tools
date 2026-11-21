package hu.pelyh.aoptools;

/** Geometry in Minecraft's scaled GUI pixels. */
record EditorLayout(int width, int height) {
    boolean compact() { return width < 500; }
    int navWidth() { return compact() ? 16 : 22; }
    int addWidth() { return compact() ? 24 : 44; }
    int deleteWidth() { return compact() ? 36 : 64; }
    int activeWidth() { return compact() ? 48 : 80; }
    int autoRunWidth() { return compact() ? 70 : 100; }
    int autoRunX() { return width - 12 - autoRunWidth(); }
    int presetNameWidth() {
        return width - 24 - 6 * 4 - 2 * navWidth() - addWidth() - deleteWidth() - activeWidth() - autoRunWidth();
    }
    int paletteWidth() { return Math.min(320, (width - 34) * 47 / 100); }
    int workspaceX() { return 22 + paletteWidth(); }
    int bodyTop() { return 68; }
    int bodyBottom() { return height - 46; }
}
