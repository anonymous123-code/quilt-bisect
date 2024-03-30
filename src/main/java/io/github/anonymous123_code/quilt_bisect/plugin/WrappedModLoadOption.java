package io.github.anonymous123_code.quilt_bisect.plugin;

import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.Version;
import org.quiltmc.loader.api.gui.QuiltLoaderIcon;
import org.quiltmc.loader.api.gui.QuiltLoaderText;
import org.quiltmc.loader.api.plugin.ModContainerExt;
import org.quiltmc.loader.api.plugin.ModMetadataExt;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;
import org.quiltmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.quiltmc.loader.api.plugin.solver.ModLoadOption;
import org.quiltmc.loader.api.plugin.solver.QuiltFileHasher;

import java.io.IOException;
import java.nio.file.Path;

public class WrappedModLoadOption extends ModLoadOption {
	private final ModLoadOption inner;
	private final boolean mandatory;
	private final ModMetadataExt.ModLoadType loadType;

	public WrappedModLoadOption(ModLoadOption inner, boolean mandatory, @Nullable ModMetadataExt.ModLoadType loadType) {
		this.inner = inner;
		this.mandatory = mandatory;
		this.loadType = loadType;
	}

	@Override
	public QuiltPluginContext loader() {
		return inner.loader();
	}

	@Override
	public ModMetadataExt metadata() {
		return new WrappedModMetadataExt(inner.metadata(), this.loadType);
	}

	@Override
	public Path from() {
		return inner.from();
	}

	@Override
	public Path resourceRoot() {
		return inner.resourceRoot();
	}

	@Override
	public boolean isMandatory() {
		return mandatory;
	}

	@Override
	public @Nullable String namespaceMappingFrom() {
		return inner.namespaceMappingFrom();
	}

	@Override
	public boolean needsTransforming() {
		return inner.needsTransforming();
	}

	@Override
	public byte[] computeOriginHash(QuiltFileHasher hasher) throws IOException {
		return inner.computeOriginHash(hasher);
	}

	@Override
	public QuiltLoaderIcon modFileIcon() {
		return inner.modFileIcon();
	}

	@Override
	public QuiltLoaderIcon modTypeIcon() {
		return inner.modTypeIcon();
	}

	@Override
	public ModContainerExt convertToMod(Path transformedResourceRoot) {
		return inner.convertToMod(transformedResourceRoot);
	}

	@SuppressWarnings("deprecation")
	@Override
	public String shortString() {
		return inner.shortString();
	}

	@SuppressWarnings("deprecation")
	@Override
	public String getSpecificInfo() {
		return inner.getSpecificInfo();
	}

	@Override
	public QuiltLoaderText describe() {
		return inner.describe();
	}

	@Override
	public String group() {
		return inner.group();
	}

	@Override
	public String id() {
		return inner.id();
	}

	@Override
	public Version version() {
		return inner.version();
	}

	@Override
	public QuiltLoaderIcon modCompleteIcon() {
		return inner.modCompleteIcon();
	}

	@Override
	public void populateModsTabInfo(PluginGuiTreeNode guiNode) {
		inner.populateModsTabInfo(guiNode);
	}

	@Override
	public String toString() {
		return inner.toString();
	}

	@SuppressWarnings("deprecation")
	@Override
	public String fullString() {
		return inner.fullString();
	}

	@SuppressWarnings("deprecation")
	@Override
	public String getLoadSource() {
		return inner.getLoadSource();
	}

	@Override
	public boolean couldResourcesChange() {
		return inner.couldResourcesChange();
	}
}
