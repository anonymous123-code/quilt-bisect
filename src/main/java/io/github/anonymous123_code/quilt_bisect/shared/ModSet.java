package io.github.anonymous123_code.quilt_bisect.shared;

import java.util.ArrayList;
import java.util.List;

public abstract class ModSet {
	protected ModSet(boolean working, ArrayList<String> modSet) {
		this.working = working;
		this.invalidated = false;
		this.modSet = modSet;
	}

	public boolean isWorking() {
		return working;
	}

	public final boolean working;
	public boolean invalidated;
	public final ArrayList<String> modSet;

	public ModSetSection getFullSection() {
		return new ModSetSection(0, this.modSet.size(), this);
	}

	public static class WorkingModSet extends ModSet {
		public WorkingModSet(ArrayList<String> modSet) {
			super(true, modSet);
		}
	}

	public static class ErroringModSet extends ModSet {
		public final int issueId;
		public final String crashLogPath;

		public ErroringModSet(ArrayList<String> modSet, int issueId, String crashLogPath) {
			super(false, modSet);
			this.issueId = issueId;
			this.crashLogPath = crashLogPath;
		}
	}

	public record ModSetSection(int start, int end, ModSet modSet) {

		public List<String> getArrayListCopy() {
			return modSet.modSet.subList(start, end);
		}

		public ModSetSection[][] getPossibleArrayHalveCombinations() {
			int middle = (start + end) / 2;
			return new ModSetSection[][] {
				new ModSetSection[] {new ModSetSection(start, middle, modSet), new ModSetSection(middle, end, modSet)},
				new ModSetSection[] {new ModSetSection(start, middle + size() % 2, modSet), new ModSetSection(middle + size() % 2, end, modSet)},
			};
		}

		public int size() {
			return end - start;
		}
	}
}
