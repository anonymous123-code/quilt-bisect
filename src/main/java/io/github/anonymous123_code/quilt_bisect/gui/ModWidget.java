package io.github.anonymous123_code.quilt_bisect.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.anonymous123_code.quilt_bisect.QuiltBisect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ModWidget extends ClickableWidget {
	public static final Style MOD_ID_STYLE = Style.EMPTY.withColor(Formatting.GRAY);
	public static final Style DESCRIPTION_STYLE = Style.EMPTY.withFormatting(Formatting.AQUA);
	public static final Style PATH_STYLE = Style.EMPTY.withFormatting(Formatting.GRAY);
	private final static int fontHeight = MinecraftClient.getInstance().textRenderer.fontHeight;
	public final IconStore iconStore;
	private final ModContainer modContainer;
	protected Identifier iconLocation;

	public ModWidget(int x, int y, ModContainer modContainer, IconStore iconStore) {
		super(x, y, height(), height(), Text.of(modContainer.metadata().name()));
		this.setWidth(fontHeight + MinecraftClient.getInstance().textRenderer.getWidth(this.getMessage()) + 3);
		this.modContainer = modContainer;
		this.iconStore = iconStore;
		this.setTooltip(buildTooltip(modContainer));
	}

	private static Tooltip buildTooltip(ModContainer mod) {
		return Tooltip.create(Text.of(mod.metadata().name() + "\n")
								  .copy()
								  .append(Text.of(mod.metadata().id() + "\n")
											  .setStyle(MOD_ID_STYLE)
											  .get(0))
								  .append(Text.of(mod.metadata().description() + "\n\n")
											  .setStyle(DESCRIPTION_STYLE)
											  .get(0))
								  .append(Text.of(mod.getSourcePaths()
													 .stream()
													 .map(ModWidget::preparePath)
													 .collect(Collectors.joining("\n\n")))
											  .setStyle(PATH_STYLE)
											  .get(0)));
	}

	private static String preparePath(List<Path> nestedPaths) {
		StringBuilder result = new StringBuilder(relativizePath(nestedPaths.get(0)));
		for (int i = 1; i < nestedPaths.size(); i++) {
			Path path = nestedPaths.get(i);
			result.append("///");
			result.append(path.toString());
		}
		return result.toString();
	}

	public static int height() {
		return fontHeight + 2;
	}

	public static String relativizePath(Path path) {
		var gameDir = QuiltLoader.getGameDir().toAbsolutePath().normalize();
		var modsDir = gameDir.resolve("mods");

		String fsSep = path.getFileSystem().getSeparator();
		path = path.toAbsolutePath().normalize();
		if (path.startsWith(modsDir)) {
			return "<mods>" + fsSep + modsDir.relativize(path);
		}
		if (path.startsWith(gameDir)) {
			return "<game>" + fsSep + gameDir.relativize(path);
		}
		String pathStr = path.toString();
		String userHome = System.getProperty("user.home");
		if (userHome.isEmpty()) {
			return pathStr;
		}
		if (!userHome.endsWith(fsSep)) {
			userHome += fsSep;
		}
		if (pathStr.startsWith(userHome)) {
			return "<user>" + fsSep + pathStr.substring(userHome.length());
		}
		return pathStr;
	}

	@Override
	protected void drawWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		//noinspection SuspiciousNameCombination
		graphics.drawTexture(
			this.getIconTexture(),
			this.getX() + 1,
			this.getY() + 1,
			0.0F,
			0.0F,
			fontHeight,
			fontHeight,
			fontHeight,
			fontHeight
		);
		RenderSystem.disableBlend();
		graphics.drawShadowedText(
			MinecraftClient.getInstance().textRenderer,
			this.getMessage(),
			this.getX() + fontHeight + 3,
			this.getY() + 2,
			0xffffff
		);
	}

	@Override
	protected void updateNarration(NarrationMessageBuilder builder) {

	}

	public Identifier getIconTexture() {
		if (this.iconLocation == null) {
			this.iconLocation = new Identifier(QuiltBisect.MOD_ID, modContainer.metadata().id() + "_icon");
			NativeImageBackedTexture icon = iconStore.getIcon(
				modContainer,
				64 * MinecraftClient.getInstance().options.getGuiScale().get()
			);
			MinecraftClient.getInstance().getTextureManager().registerTexture(this.iconLocation, icon);
		}
		return iconLocation;
	}
}
