package at.jku.isse.gitecco.core.test;

import at.jku.isse.gitecco.core.type.Feature;
import org.junit.Test;

public class CoreTest {

	@Test
	public void ConditionParserTest() {
		String exp = "!a && !b && d==0 && __TEST__";
		Feature.parseCondition(exp).forEach(x -> System.out.println(x.getName()));
	}

}
