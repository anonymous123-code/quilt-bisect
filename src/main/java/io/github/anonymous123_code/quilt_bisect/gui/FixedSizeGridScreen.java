package io.github.anonymous123_code.quilt_bisect.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class FixedSizeGridScreen extends Screen {
	protected final int ROW_COUNT;
	protected final int COLUMN_COUNT;
	protected final int CELL_HEIGHT;
	protected final int CELL_WIDTH;
	protected final int HORIZONTAL_PADDING;
	protected final int VERTICAL_PADDING;

	protected FixedSizeGridScreen(Text title, int rowCount, int columnCount, int cellHeight, int cellWidth, int horizontalPadding, int verticalPadding) {
		super(title);
		ROW_COUNT = rowCount;
		COLUMN_COUNT = columnCount;
		CELL_HEIGHT = cellHeight;
		CELL_WIDTH = cellWidth;
		HORIZONTAL_PADDING = horizontalPadding;
		VERTICAL_PADDING = verticalPadding;
	}

	protected int cornerX(int column) {
		int perElementWidth = CELL_WIDTH + HORIZONTAL_PADDING;
		return width / 2 - (perElementWidth * COLUMN_COUNT / 2 - HORIZONTAL_PADDING / 2) + (perElementWidth * column);
	}

	protected int cornerY(int row) {
		int perElementHeight = CELL_HEIGHT + VERTICAL_PADDING;
		return height / 2 - (perElementHeight * ROW_COUNT / 2 - VERTICAL_PADDING / 2) + (perElementHeight * row);
	}

	protected int centerX(int column, int width) {
		return cornerX(column) + (CELL_WIDTH - width) / 2;
	}

	protected int centerY(int row, int height) {
		return cornerY(row) + (CELL_HEIGHT - height) / 2;
	}
}
