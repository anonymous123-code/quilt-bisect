package io.github.anonymous123_code.quilt_bisect.gui;

import com.google.common.collect.Sets;
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
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ModWidget extends ClickableWidget {
	public static final Style MOD_ID_STYLE = Style.EMPTY.withColor(Formatting.GRAY);
	public static final Style DESCRIPTION_STYLE = Style.EMPTY.withFormatting(Formatting.AQUA);
	public static final Style PATH_STYLE = Style.EMPTY.withFormatting(Formatting.GRAY);
	private static final Set<String> ALLOWED_PROTOCOLS = Sets.newHashSet("http", "https");
	private final static int fontHeight = MinecraftClient.getInstance().textRenderer.fontHeight;

	public final IconStore iconStore;
	private final ClickBehaviorHolder clickBehaviorHolder;
	private final ModContainer mod;
	protected Identifier iconLocation;
	@Nullable
	private final String homepage;
	@Nullable
	private final String issues;
	@Nullable
	private final String sources;

	public ModWidget(int x, int y, ClickBehaviorHolder clickBehaviorHolder, ModContainer mod, IconStore iconStore) {
		super(x, y, height(), height(), Text.of(mod.metadata().name()));
		this.clickBehaviorHolder = clickBehaviorHolder;
		this.setWidth(fontHeight + MinecraftClient.getInstance().textRenderer.getWidth(this.getMessage()) + 3);
		this.mod = mod;
		this.iconStore = iconStore;
		this.homepage = mod.metadata().getContactInfo("homepage");
		this.issues = mod.metadata().getContactInfo("issues");
		this.sources = mod.metadata().getContactInfo("sources");
		this.setTooltip(buildTooltip());
	}

	private static void openFile(Path path) {
		// TODO: This will open the file and decompress it, instead of showing it in the file manager
		URI uRI = path.toFile().toURI();
		openLink(uRI);
	}

	private static void openUrl(String url) {
		try {
			URI uRI = new URI(url);
			String string = uRI.getScheme();
			if (string == null) {
				throw new URISyntaxException(url, "Missing protocol");

			}

			if (!ALLOWED_PROTOCOLS.contains(string.toLowerCase(Locale.ROOT))) {
				throw new URISyntaxException(url, "Unsupported protocol: " + string.toLowerCase(Locale.ROOT));
			}

			openLink(uRI);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static void copy(String text) {
		MinecraftClient.getInstance().keyboard.setClipboard(text);
	}

	private static void openLink(URI link) {
		Util.getOperatingSystem().open(link);
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

	private Tooltip buildTooltip() {
		return Tooltip.create(Text.of(mod.metadata().name() + "\n")
								  .copy()
								  .append(Text.of(mod.metadata().id() + "\n")
											  .setStyle(MOD_ID_STYLE)
											  .get(0))
								  .append(Text.of(mod.metadata().description() + "\n\n")
											  .setStyle(DESCRIPTION_STYLE)
											  .get(0))
								  .append(Text.of(homepage + "\n"))
								  .append(Text.of(issues + "\n"))
								  .append(Text.of(sources + "\n\n"))
								  .append(Text.of(mod.getSourcePaths()
													 .stream()
													 .map(ModWidget::preparePath)
													 .collect(Collectors.joining("\n\n")))
											  .setStyle(PATH_STYLE)
											  .get(0)));
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		switch (clickBehaviorHolder.inner) {
			case CopyId -> copy(mod.metadata().id());
			case CopyName -> copy(mod.metadata().name());
			case CopyPath -> copy(mod.getSourcePaths().get(0).get(0).toString());
			case CopyIssues -> copy(issues);
			case CopySources -> copy(sources);
			case CopyHomepage -> copy(homepage);
			case OpenPath -> openFile(mod.getSourcePaths().get(0).get(0));
			case OpenIssues -> openUrl(issues);
			case OpenHomepage -> openUrl(homepage);
			case OpenSources -> openUrl(sources);
		}
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
			this.iconLocation = new Identifier(QuiltBisect.MOD_ID, mod.metadata().id() + "_icon");
			NativeImageBackedTexture icon = iconStore.getIcon(
				mod,
				64 * MinecraftClient.getInstance().options.getGuiScale().get()
			);
			MinecraftClient.getInstance().getTextureManager().registerTexture(this.iconLocation, icon);
		}
		return iconLocation;
	}

	public enum ClickBehavior {
		OpenPath,
		OpenHomepage,
		OpenIssues,
		OpenSources,
		CopyPath,
		CopyHomepage,
		CopyIssues,
		CopySources,
		CopyName,
		CopyId;

		public ClickBehavior next() {
			return switch (this) {
				case OpenPath -> OpenHomepage;
				case OpenHomepage -> OpenIssues;
				case OpenIssues -> OpenSources;
				case OpenSources -> CopyPath;
				case CopyPath -> CopyHomepage;
				case CopyHomepage -> CopyIssues;
				case CopyIssues -> CopySources;
				case CopySources -> CopyName;
				case CopyName -> CopyId;
				case CopyId -> OpenPath;
			};
		}

		public Text text() {
			return switch (this) {
				case OpenPath -> Text.of("Open path");
				case OpenHomepage -> Text.of("Open homepage");
				case OpenIssues -> Text.of("Open issues");
				case OpenSources -> Text.of("Open sources");
				case CopyPath -> Text.of("Copy path");
				case CopyHomepage -> Text.of("Copy homepage");
				case CopyIssues -> Text.of("Copy issues");
				case CopySources -> Text.of("Copy sources");
				case CopyName -> Text.of("Copy name");
				case CopyId -> Text.of("Copy id");
			};
		}
	}

	public static class ClickBehaviorHolder {
		public ClickBehavior inner;

		public ClickBehaviorHolder(ClickBehavior inner) {
			this.inner = inner;
		}
	}
}
