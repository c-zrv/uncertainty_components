import java.io.File;

import uk.ac.nactem.uima.test.CasProcessorTester;

public class UncertaintyListingTester {

	
	public static void main(String[] args) throws Exception {
		System.setProperty("uima.datapath", "resources");

		//get all files to be processed
		File directory =new File("/home/chryssa/PhD/LeukemiaR/");
		File [] files = directory.listFiles();

		System.out.println("NUMBER OF FILES: "+files.length);

		//process in bunches or altogether
		int max = 500;
		int start = 0;
		boolean finish = false;
		while(!finish){
			CasProcessorTester tester = new CasProcessorTester("desc/uk/ac/nactem/uima/UncertaintyListing.xml");
			tester.setParam("OutputFile", "resources/leukemiaR"+Integer.toString(start)+".tsv");
			//process next bunch
			for(int i = start; i<Math.min(files.length, start+max); i++){
				File file  = files[i];
				System.out.println(file.getName());
				if(file.getName().endsWith("xmi")){
					tester.addInput(file);	
				}
			}
			tester.process();
			System.out.println(start);
			start = start+max;
			if(start>=files.length-1)
				finish = true;
		}
	}
	
	
}
