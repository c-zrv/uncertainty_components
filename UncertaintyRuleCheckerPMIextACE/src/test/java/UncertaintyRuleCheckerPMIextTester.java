import java.io.File;

import uk.ac.nactem.uima.test.CasProcessorTester;

public class UncertaintyRuleCheckerPMIextTester {
	public static void main(String[] args) throws Exception {
		System.setProperty("uima.datapath", "resources");
		CasProcessorTester tester = new CasProcessorTester("desc/uk/ac/nactem/uima/UncertaintyRuleCheckerPMIext.xml");
		tester.setParam("InputFile", "resources/clues_rules_ace.txt");
		tester.setParam("RuleLogFile", "resources/ruleLog.txt");
		File directory =new File("/home/chryssa/PhD/PROPMIXMIs");
		File [] files = directory.listFiles();

		System.out.println("NUMBER OF FILES: "+files.length);

		for(File file : files){
			System.out.println(file.getName());
			tester.addInput(file);
		}
		tester.process();
	}
}
