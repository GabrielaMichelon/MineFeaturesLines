package at.jku.isse.gitecco.core.type;

/**
 * Class for representing a feature implication through a #define
 *
 * Example:
 * #if A && B
 *  #define C 8
 * #endif
 *
 * would be modeled like this: new FeatureImplication("A &&B ", "C == 8");
 *
 * For boolean values (#define A) it is enough to enter "A" as the value.
 *
 */
public class FeatureImplication {
    String condition, value;

    public FeatureImplication(String condition, String value) {
        this.condition = condition;
        this.value = value;
    }

    public String getCondition() {
        return condition;
    }

    //C == 8 --> #define C 8
    public String getValue() {
        return value;
    }

}
