package io.github.anonymous123_code.quilt_bisect.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class ModSet {
	public final ArrayList<String> modSet;
	public final List<Integer> sections;
	public boolean invalidated;
	protected ModSet(ArrayList<String> modSet, ArrayList<Integer> sections) {
		this.sections = sections;
		this.invalidated = false;
		this.modSet = modSet;
	}

	public SectionIterator iterator() {
		return new SectionIterator();
	}

	public Section getFullSection() {
		return new Section(0, this.modSet.size(), this);
	}

	public abstract boolean isWorkingOrFixed(ActiveBisectConfig activeBisectConfig);

	public static class Working extends ModSet {
		public Working(ArrayList<String> modSet, ArrayList<Integer> sections) {
			super(modSet, sections);
		}

		@Override
		public boolean isWorkingOrFixed(ActiveBisectConfig activeBisectConfig) {
			return true;
		}
	}

	public static class Erroring extends ModSet {
		public final int issueId;
		public final String crashLogPath;

		public Erroring(ArrayList<String> modSet, int issueId, String crashLogPath, ArrayList<Integer> sections) {
			super(modSet, sections);
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

	public class SectionIterator implements Iterator<Section> {
		private int sectionIndex = 0;

		@Override
		public boolean hasNext() {
            return sectionIndex < sections.size();
		}

		@Override
		public Section next() {
			if (!hasNext()) throw new NoSuchElementException();
			sectionIndex++;
			if (sectionIndex == sections.size()) {
                return new Section(sections.get(sectionIndex - 1), modSet.size(), ModSet.this);
			} else {
                return new Section(sections.get(sectionIndex - 1), sections.get(sectionIndex), ModSet.this);
			}
		}
	}
}
