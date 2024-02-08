package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import net.minecraft.text.Text;

public abstract class FixedSizeGridScreen extends SpruceScreen {
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

	protected Position corner(int row, int column) {
		int perElementWidth = CELL_WIDTH + HORIZONTAL_PADDING;
		int perElementHeight = CELL_HEIGHT + VERTICAL_PADDING;
		return Position.of(
			width / 2 - (perElementWidth * COLUMN_COUNT / 2) + (perElementWidth * column),
			height / 2 - (perElementHeight * ROW_COUNT / 2) + (perElementHeight * row)
		);
	}

	protected Position centerVertically(int row, int column, int height) {
		return Position.of(corner(row, column), 0, (CELL_HEIGHT - height) / 2);
	}

	protected Position centerHorizontally(int row, int column, int width) {
		return Position.of(corner(row, column), (CELL_WIDTH - width) / 2, 0);
	}

	protected Position center(int row, int column, int width, int height) {
		return Position.of(corner(row, column), (CELL_WIDTH - width) / 2, (CELL_HEIGHT - height) / 2);
	}
}
