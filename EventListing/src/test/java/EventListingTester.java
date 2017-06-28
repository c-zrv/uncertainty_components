import java.io.File;
import java.util.Arrays;

import uk.ac.nactem.uima.test.CasProcessorTester;

public class EventListingTester {
	public static void main(String[] args) throws Exception{

		System.setProperty("uima.datapath", "resources");

		CasProcessorTester tester = new CasProcessorTester("desc/uk/ac/nactem/uima/EventListing.xml");

		File directory =new File("/home/chryssa/PhD/LitPathExplorer_PubMed_Events_Grounding_MK_CM");
		File [] files = directory.listFiles();
		Arrays.sort(files);
		tester.setParam("OutFile", "resources/out_ab.tsv");
		System.out.println("NUMBER OF FILES: "+files.length);
	//	tester.addInput("/home/chryssa/PhD/BC_12K_sample/A_Ras-2-neighborhood_synonyms.xmi");
	//	tester.process();
		for(File file : files){
			tester.addInput(file);
		}
		tester.setUseCasVisualDebugger();
		tester.process();
	//	tester.addInput("/home/chryssa/PhD/BC_12K_sample/Z_Ras-2-neighborhood_synonyms.xmi");
	//	tester.process();
		tester.setUseCasVisualDebugger();
	}
}
