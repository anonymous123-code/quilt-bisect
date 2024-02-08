package io.github.anonymous123_code.quilt_bisect.gui;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import net.minecraft.text.Text;

public abstract class FixedSizeGridScreen extends SpruceScreen {
	protected final int ROW_COUNT;
	protected final int COLUMN_COUNT;
	protected final int ROW_HEIGHT;
	protected final int COLUMN_WIDTH;
	protected final int HORIZONTAL_PADDING;
	protected final int VERTICAL_PADDING;

	protected FixedSizeGridScreen(Text title, int rowCount, int columnCount, int rowHeight, int columnWidth, int horizontalPadding, int verticalPadding) {
		super(title);
		ROW_COUNT = rowCount;
		COLUMN_COUNT = columnCount;
		ROW_HEIGHT = rowHeight;
		COLUMN_WIDTH = columnWidth;
		HORIZONTAL_PADDING = horizontalPadding;
		VERTICAL_PADDING = verticalPadding;
	}

	protected Position corner(int row, int column) {
		return Position.of(
			this.width / 2 - (COLUMN_WIDTH * COLUMN_COUNT / 2) + (COLUMN_WIDTH * column),
			this.height / 2 - (ROW_HEIGHT * ROW_COUNT / 2) + (ROW_HEIGHT * row)
		);
	}

	protected Position centerVertically(int row, int column, int height) {
		return Position.of(corner(row, column), 0, (ROW_HEIGHT - height) / 2);
	}

	protected Position centerHorizontally(int row, int column, int width) {
		return Position.of(corner(row, column), (COLUMN_WIDTH - width) / 2, 0);
	}

	protected Position center(int row, int column, int width, int height) {
		return Position.of(corner(row, column), (COLUMN_WIDTH - width) / 2, (ROW_HEIGHT - height) / 2);
	}
}
