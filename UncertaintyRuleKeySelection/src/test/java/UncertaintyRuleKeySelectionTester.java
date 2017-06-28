import java.io.File;

import uk.ac.nactem.uima.test.CasProcessorTester;


public class UncertaintyRuleKeySelectionTester {

	public static void main(String[] args) throws Exception {
		System.setProperty("uima.datapath", "resources");
		

		File directory =new File("/home/chryssa/PhD/bionlpSTEnju");
		File [] files = directory.listFiles();

		System.out.println("NUMBER OF FILES: "+files.length);
		int count = 0;
		CasProcessorTester tester = new CasProcessorTester("desc/uk/ac/nactem/uima/UncertaintyRuleKeySelection.xml");
		tester.setParam("CueFile", "resources/lemmas.txt");
		tester.setParam("RuleFile", "resources/clues_rulesn.txt");
		tester.setParam("PMIFile", "resources/clues_rules_pmin.txt");
		for(File file : files){
			
	

			System.out.println(file.getName());
			tester.addInput(file);
			//tester.setUseCasVisualDebugger();
			
			count++;
		//	if(count>0)
		//		break;

		}
		tester.process();
//		tester.setUseCasVisualDebugger();
//		tester.process();
		


	}


}


