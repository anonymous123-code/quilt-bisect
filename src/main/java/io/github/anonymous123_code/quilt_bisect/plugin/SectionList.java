package io.github.anonymous123_code.quilt_bisect.plugin;

import io.github.anonymous123_code.quilt_bisect.shared.ModSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record SectionList(List<String> modIds, List<Integer> sectionIndices) {
	public static SectionList fromSections(List<ModSet.Section> sections) {
		sections.sort(Comparator.comparingInt(ModSet.Section::start));

		var sectionIndices = new ArrayList<Integer>(sections.size());
		sectionIndices.add(0);
		var flatMapModIds = new ArrayList<String>();
		for (ModSet.Section modSection : sections) {
			sectionIndices.add(flatMapModIds.size() + modSection.size());
			flatMapModIds.addAll(modSection.getListCopy());
		}
		sectionIndices.remove(sectionIndices.size() - 1);

		return new SectionList(flatMapModIds, sectionIndices);
	}

	public static SectionList from(ArrayList<String> mods) {
		return new SectionList(mods, List.of(0));
	}

	public static SectionList from(Collection<String> mods) {
		return new SectionList(new ArrayList<>(mods), List.of(0));
	}
}
