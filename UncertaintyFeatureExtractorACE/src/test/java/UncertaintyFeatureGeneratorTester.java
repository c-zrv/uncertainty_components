import java.io.File;

import uk.ac.nactem.uima.test.CasProcessorTester;


public class UncertaintyFeatureGeneratorTester {
	public static void main(String[] args) throws Exception {
		System.setProperty("uima.datapath", "resources");
		
		File directory =new File("/home/chryssa/PhD/genia4xmis");
		File [] files = directory.listFiles();
		
		
		CasProcessorTester tester = new CasProcessorTester("desc/uk/ac/nactem/uima/UncertaintyFeatureGeneratorACE.xml");
		tester.setParam("CueFile", "resources/lemmas.genia.train.5.txt");
		
		for(File file : files){
			System.out.println(file.getName());
			tester.addInput(file);
		}
		tester.process();
		
		tester.setUseCasVisualDebugger();

	}
}
