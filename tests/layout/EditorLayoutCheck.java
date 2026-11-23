package hu.pelyh.aoptools;

/**
 * Run with tests/layout/verify.ps1, or compile this and EditorLayout.java
 * together with javac and run hu.pelyh.aoptools.EditorLayoutCheck.
 */
public final class EditorLayoutCheck {
    public static void main(String[] args) {
        int checked = 0;
        for (int width = 320; width <= 960; width++) {
            for (int height : new int[] {240, 241, 270, 360, 480, 540}) {
                verify(new EditorLayout(width, height));
                checked++;
            }
        }
        System.out.println("Editor layout passed for " + checked + " scaled viewport sizes.");
    }

    private static void verify(EditorLayout layout) {
        int right = layout.width() - 12;
        int paletteRight = 12 + layout.paletteWidth();
        int toolbarX = 12;
        for (int controlWidth : new int[] {layout.navWidth(), layout.presetNameWidth(),
                layout.navWidth(), layout.addWidth(), layout.deleteWidth(), layout.activeWidth()}) {
            require(layout, controlWidth > 0 && toolbarX + controlWidth < right, "toolbar control clipped");
            toolbarX += controlWidth + 4;
        }
        require(layout, toolbarX == layout.autoRunX(), "toolbar controls overlap or leave an unintended gap");
        require(layout, layout.presetNameWidth() > 8, "preset name field has no typing area");
        require(layout, layout.autoRunWidth() > 8 && layout.autoRunX() >= 12
                && layout.autoRunX() + layout.autoRunWidth() == right, "autorun control outside toolbar");
        require(layout, layout.paletteWidth() >= 80 && layout.workspaceX() - paletteRight >= 1,
                "palette and workspace overlap");
        require(layout, right - layout.workspaceX() >= 80, "workspace too narrow for a block");
        require(layout, layout.bodyTop() >= 63, "toolbar overlaps editor body");
        // Search + target/block + kick/ban reason controls and one two-line palette block.
        require(layout, layout.bodyBottom() - layout.bodyTop() >= 82 + 2 + 28,
                "palette cannot display a complete two-line block");
        require(layout, layout.bodyBottom() <= layout.height() - 41, "body overlaps footer");
    }

    private static void require(EditorLayout layout, boolean condition, String message) {
        if (!condition) throw new AssertionError(layout.width() + "x" + layout.height() + ": " + message);
    }
}
