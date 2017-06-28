import java.io.File;

import uk.ac.nactem.uima.test.CasProcessorTester;


public class UncertaintyRuleKeySelectionBroadTester {

	public static void main(String[] args) throws Exception {
		System.setProperty("uima.datapath", "resources");
		

		File directory =new File("/home/chryssa/PhD/bionlp/All_BioNLP_train_1_enju");
		File [] files = directory.listFiles();

		System.out.println("NUMBER OF FILES: "+files.length);
		int count = 0;
		CasProcessorTester tester = new CasProcessorTester("desc/uk/ac/nactem/uima/UncertaintyRuleKeySelectionBroad.xml");
		tester.setParam("RuleFile", "resources/ace_bio.1.txt");
		tester.setParam("PMIFile", "resources/ace_bio.1.pmi.tsv");
		tester.setParam("LiftFilter", "11");
		for(File file : files){
			
	

			System.out.println(file.getName());
			tester.addInput(file);
	
			count++;

		}
		tester.process();

		


	}


}


