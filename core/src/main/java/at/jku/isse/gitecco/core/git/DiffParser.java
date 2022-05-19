package at.jku.isse.gitecco.core.git;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing diffs for changes.
 */
public class DiffParser {

    private static final Pattern HUNK_START_PATTERN =
            Pattern.compile("@@ -([0-9]+)(?:,?([0-9]?))? \\+([0-9]+)(?:,?([0-9]?)) @@");

    private final List<Change> minusChanges;
    private final List<Change> plusChanges;

    /**
     * Creates a new instance of the DiffParser.
     */
    public DiffParser() {
        minusChanges = new ArrayList<Change>();
        plusChanges = new ArrayList<Change>();
    }

    /**
     * Parses a given Diff for relevant changes
     * and extracts the necessary information form the diff.
     *
     * @param diff String of the diff.
     * @return <code>true</code> if the parsing was successful,
     * <code>flase</code> if nothing was found.
     */
    public boolean parse(String diff) {

        Matcher matcher = HUNK_START_PATTERN.matcher(diff);
        Matcher smallMatcher;
        boolean ret = false;

        //System.out.println(diff);

        List<String> allMatches = new ArrayList<String>();
        while (matcher.find()) {
            allMatches.add(matcher.group());
        }

        for (String match : allMatches) {
            smallMatcher = HUNK_START_PATTERN.matcher(match);
            if (smallMatcher.matches()) {
                try {
                    String range1Start = smallMatcher.group(1);
                    String range1Count = (!smallMatcher.group(2).isEmpty()) ? smallMatcher.group(2) : "1";
                    ArrayList<Integer> lines = new ArrayList<>();
                    minusChanges.add(new Change(Integer.valueOf(range1Start), Integer.valueOf(range1Count), lines, -1,null));

                    String range2Start = smallMatcher.group(3);
                    String range2Count = (!smallMatcher.group(4).isEmpty()) ? smallMatcher.group(4) : "1";
                    plusChanges.add(new Change(Integer.valueOf(range2Start), Integer.valueOf(range2Count),lines, -1,null));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ret = true;
            }
        }
        return ret;
    }

    /**
     * Returns all the minus Changes.
     * In this case of use most likely not relevant.
     *
     * @return Array of all the minus changes.
     */
    public Change[] getminusRanges() {
        return minusChanges.toArray(new Change[minusChanges.size()]);
    }

    /**
     * Returns all the plus Changes.
     *
     * @return Array of all the plus changes.
     */
    public Change[] getplusRanges() {
        return plusChanges.toArray(new Change[plusChanges.size()]);
    }

    /**
     * Resets the parser.
     * <br>Do this after every diff string parsed. <br>
     * Needed, so that after parsing one diff, the same
     * parser instance can be used again for another diff.
     */
    public void reset() {
        minusChanges.clear();
        plusChanges.clear();
    }
}
