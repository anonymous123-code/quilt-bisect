package io.github.anonymous123_code.quilt_bisect.shared;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;

public class MainConfig extends AbstractBisectConfig {
	public static final MainConfig INSTANCE = create(MainConfig.class);

	/**
	 * The mod ids bisect ignores does not unload when not bisecting. Removing one of the defaults may cause issues. Default ["quilt_bisect", "minecraft", "java", "quilt_loader"]
	 */
	public ArrayList<String> bisectSkipMods = new ArrayList<>(Arrays.asList(
		"quilt_bisect",
		"minecraft",
		"java",
		"quilt_loader"
	));
	/**
	 * The default settings for a new bisect. The auto join name and auto join type are ignored in favor of the last joined.
	 */
	public AutoTest defaultBisectSettings = new AutoTest(AutoTest.AutoJoinType.None, "", "", false, 20 * 5, false);

	/**
	 * When now exact match is found, show diff popup
	 * <li>Always</li>
	 * <li>Never</li>
	 * <li>FirstLineMatch: When the first line of a known stacktrace matches</li>
	 */
	public ShowDiffOptions showDiffWhenNoExactMatchFound = ShowDiffOptions.FirstLineMatch;

	/**
	 * The place vanilla stores its crash reports. Do not change unless you know what you are doing. Default "crash-reports"
	 */
	public String crashReportsDirectory = "crash-reports";
	/**
	 * The place bisect stores the logs of all past runs in which it were active
	 */
	public String bisectLogStorageDirectory = "bisect_log_storage";
	/**
	 * The place bisect stores the backups for the saves when world saving is disabled
	 */
	public String bisectSavesBackupDirectory = "bisect_saves_backup";

	protected MainConfig() {
		super("main");
	}

	@Override
	public void setupReadGson(GsonBuilder builder) {
	}

	public enum ShowDiffOptions {
		Always,
		Never,
		FirstLineMatch,
	}
}
