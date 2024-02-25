package io.github.anonymous123_code.quilt_bisect.plugin;

public class Diff {

	/**
	 * Calculates the longest common sequence between the two strings.
	 * <p>
	 * See <a href="https://pubs.dbs.uni-leipzig.de/se/files/Myers1986AnONDDifferenceAlgorithm.pdf">An O(ND) Difference Algorithm and its Variations by Eugene W. Myers</a>
	 */
	public static String calculateLongestCommonSequence(String string1, String string2) {
		int string1Length = string1.codePointCount(0, string1.length());
		int string2Length = string2.codePointCount(0, string2.length());
		int diagonalCount = string1Length + string2Length;
		DiagonalData[] data = new DiagonalData[diagonalCount * 2 + 1];
		data[diagonalCount + 1] = new DiagonalData(0, "");
		for (int distance = 0; distance <= diagonalCount; distance++) {
			for (int diagonal = -distance; diagonal <= distance; diagonal += 2) {
				int string1index;
				String common;
				if (diagonal == -distance || (diagonal != distance && data[diagonalCount + diagonal - 1].string1index < data[diagonalCount + diagonal + 1].string1index)) {
					string1index = data[diagonalCount + diagonal + 1].string1index;
					common = data[diagonalCount + diagonal + 1].common;
				} else {
					string1index = data[diagonalCount + diagonal - 1].string1index + 1;
					common = data[diagonalCount + diagonal - 1].common;
				}
				int string2index = string1index - diagonal;
				var newCommon = new StringBuilder(common);
				while (string1index < string1Length && string2index < string2Length && string1.codePointAt(string1index) == string2.codePointAt(string2index)) {
					newCommon.appendCodePoint(string1.codePointAt(string1index));
					string1index++;
					string2index++;
				}
				data[diagonalCount + diagonal] = new DiagonalData(string1index, newCommon.toString());
				if (string1index > string1Length && string2index > string2Length) {
					return common;
				}
			}
		}
		return "";
	}

	private record DiagonalData(int string1index, String common) {

	}
}
