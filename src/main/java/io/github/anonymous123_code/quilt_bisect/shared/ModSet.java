package io.github.anonymous123_code.quilt_bisect.shared;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ModSet {
	@SuppressWarnings("unused") // used in ActiveBisect config during serialization
	public final boolean working;
	public final ArrayList<String> modSet;
	public final List<Integer> sections;
	public boolean invalidated;
	protected ModSet(boolean working, ArrayList<String> modSet, ArrayList<Integer> sections) {
		this.working = working;
		this.sections = sections;
		this.invalidated = false;
		this.modSet = modSet;
	}

	public boolean isWorking() {
		return working;
	}

	public List<Section> sections() {
		List<Section> result = new ArrayList<>();
		for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
			if (sectionIndex + 1 == sections.size()) {
				result.add(new Section(sections.get(sectionIndex), modSet.size(), ModSet.this));
			} else {
				result.add(new Section(sections.get(sectionIndex), sections.get(sectionIndex + 1), ModSet.this));
			}
		}
		return result;
	}

	public Section getFullSection() {
		return new Section(0, this.modSet.size(), this);
	}

	public abstract boolean isWorkingOrFixed(ActiveBisectConfig activeBisectConfig);

	public static class Working extends ModSet {
		public Working(ArrayList<String> modSet, ArrayList<Integer> sections) {
			super(true, modSet, sections);
		}

		@Override
		public boolean isWorkingOrFixed(ActiveBisectConfig activeBisectConfig) {
			return true;
		}
	}

	public static class Erroring extends ModSet {
		public final int issueId;
		public final String crashLogPath;

		public Erroring(ArrayList<String> modSet, int issueId, @NotNull String crashLogPath, ArrayList<Integer> sections) {
			super(false, modSet, sections);
			this.issueId = issueId;
			this.crashLogPath = crashLogPath;
		}

		@Override
		public boolean isWorkingOrFixed(ActiveBisectConfig activeBisectConfig) {
			var issueFix = activeBisectConfig.issues.get(issueId).fix;
			return issueFix.reproductions.stream().anyMatch(modSet::containsAll);
		}
	}

	public record Section(int start, int end, ModSet modSet) {

		public List<String> getListCopy() {
			return modSet.modSet.subList(start, end);
		}

		public Section[][] getPossibleArrayHalveCombinations() {
			int middle = (start + end) / 2;
			return new Section[][]{
				new Section[]{new Section(start, middle, modSet), new Section(middle, end, modSet)},
				new Section[]{new Section(start, middle + size() % 2, modSet), new Section(middle + size() % 2, end, modSet)},
			};
		}

		public int size() {
			return end - start;
		}
	}
}
