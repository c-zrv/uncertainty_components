package uk.ac.nactem.uima;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent;
import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuSentence;
import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuToken;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.u_compare.shared.syntactic.SyntacticAnnotation;

import uk.ac.nactem.uima.cas.bionlpst.Entity;
import uk.ac.nactem.uima.cas.bionlpst.Event;




public class UncertaintyRuleKeySelectionBroad extends JCasAnnotator_ImplBase {

	private LinkedHashMap<String, Integer> clueMapD1;
	private LinkedHashMap<String, Integer> clueMapD0;
	private LinkedHashMap<String, Integer> clueMapDW;
	private LinkedHashMap<String, Integer> clueMapDN;

	private LinkedHashMap<String, Integer> unclueMapD1;
	private LinkedHashMap<String, Integer> unclueMapD0;
	private LinkedHashMap<String, Integer> unclueMapDW;
	private LinkedHashMap<String, Integer> unclueMapDN;

	private ArrayList<String> ruleKeyListD0;
	private ArrayList<String> ruleKeyListD1;
	private ArrayList<String> ruleKeyListDW;
	private ArrayList<String> ruleKeyListDN;

	public static final String PARAM_NAME_RULEFILE = "RuleFile"; //file to read rules from
	public static final String PARAM_NAME_PMIFILE = "PMIFile"; //file to read rules from
	public static final String PARAM_NAME_LIFT = "LiftFilter";
	public static final String PARAM_NAME_LEVERAGE = "LeverageFilter";
	public static final String PARAM_NAME_JACCARD = "JaccardFilter";
	public static final String PARAM_NAME_JMEASURE = "JmeasureFilter";
	//private int k;
	private File ff, clues_rules_out;
	double liftF=Double.NEGATIVE_INFINITY, levF=Double.NEGATIVE_INFINITY, jaccF=Double.NEGATIVE_INFINITY, jmF=Double.NEGATIVE_INFINITY; //filter values
	int y1count, y0count, rcount;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		try {



			ruleKeyListD0 = new ArrayList<String>();
			ruleKeyListD1 = new ArrayList<String>();
			ruleKeyListDW = new ArrayList<String>();
			ruleKeyListDN = new ArrayList<String>();


			
			if (!((String)aContext.getConfigParameterValue(PARAM_NAME_LIFT)).isEmpty()){
				liftF = Double.parseDouble((String)aContext.getConfigParameterValue(PARAM_NAME_LIFT));
			}
	/*		if (!((String)aContext.getConfigParameterValue(PARAM_NAME_LEVERAGE)).isEmpty()){
				levF = Double.parseDouble((String)aContext.getConfigParameterValue(PARAM_NAME_LEVERAGE));
			}
			if (!((String)aContext.getConfigParameterValue(PARAM_NAME_JACCARD)).isEmpty()){
				jaccF = Double.parseDouble((String)aContext.getConfigParameterValue(PARAM_NAME_JACCARD));
			}
			if (!((String)aContext.getConfigParameterValue(PARAM_NAME_JMEASURE)).isEmpty()){
				jmF = Double.parseDouble((String)aContext.getConfigParameterValue(PARAM_NAME_JMEASURE));
			}*/
			//k = 500;
			clues_rules_out = new File((String)(aContext.getConfigParameterValue(PARAM_NAME_RULEFILE)));
			ff = new File((String)(aContext.getConfigParameterValue(PARAM_NAME_PMIFILE)));
			y0count = 0; y1count = 0; rcount = 0;

			clueMapD0 = new LinkedHashMap<String, Integer>();
			clueMapD1 = new LinkedHashMap<String, Integer>();
			clueMapDW = new LinkedHashMap<String, Integer>();
			clueMapDN = new LinkedHashMap<String, Integer>();

			unclueMapD0 = new LinkedHashMap<String, Integer>();
			unclueMapD1 = new LinkedHashMap<String, Integer>();
			unclueMapDW = new LinkedHashMap<String, Integer>();
			unclueMapDN = new LinkedHashMap<String, Integer>();
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> sentenceIterator = jcas.getAnnotationIndex(EnjuSentence.type).iterator();
		while (sentenceIterator.hasNext()) {
			EnjuSentence currSentence = (EnjuSentence) sentenceIterator.next();

			FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(currSentence);
			while (eventIterator.hasNext()) {
				Event event = (Event) eventIterator.next();


				D0Arg1MW(event, currSentence, jcas);
				D0Arg2MW(event, currSentence, jcas);
				D1Arg1Arg1(event, currSentence, jcas);
				D1Arg1Arg2(event, currSentence, jcas);
				D1Arg2Arg1(event, currSentence, jcas);
				D1Arg2Arg2(event, currSentence, jcas);
				DWArg1Arg2(event, currSentence, jcas);
				DWArg2Arg1(event, currSentence, jcas);
				aolkArg1not(event, currSentence, jcas);
				aolkArg2not(event, currSentence, jcas);

				if (event.getSpeculation()){
					y1count ++;
				}
				else{
					y0count ++;
				}
			}//while eventIterator
		}//while sentenceIterator
	}


	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {

		//X = rule
		//Y = uncertainty
		double pxy, py, px, pyIFx, p_y, p_yIFx, px_y;
		double lift, leverage, jaccard, jmeasure;
		//verification
		if(!verifyCompatibility()){
			System.out.println("WARNING: Incompatible maps. "); //not needed any more
		}
		System.out.println("Collection Process Complete");
		try{

			String [] pC1names = new String [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];
			String [] stats = new String [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];


			double[] pmiC1  = new double [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];
			double[] liftArr  = new double [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];
			double[] levArr  = new double [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];
			double[] jaccArr  = new double [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];
			double[] jmArr  = new double [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];
			int  featureIndex =0;
			for(String s : ruleKeyListD0){

				int uncount = 0, ccount = 0;
				if(unclueMapD0.containsKey(s)){
					uncount = unclueMapD0.get(s);
				}

				if(clueMapD0.containsKey(s)){
					ccount = clueMapD0.get(s);
				}


				pxy = ((double) (uncount)/ (y1count+y0count));
				px = ((double) (uncount+ccount)/ (y1count+y0count));
				py = ((double) (y1count)/ (y1count+y0count));
				pyIFx = ((double) (uncount)/ (uncount+ccount));
				p_y = ((double) (y0count)/ (y1count+y0count));
				p_yIFx = ((double) (ccount)/ (uncount+ccount));
				px_y = ((double) (ccount)/ (y1count+y0count));
				lift =(double)  pxy/(px*py);
				leverage =  (double)  pxy-(px*py);
				jaccard =  (double)  pxy/(px+py-pxy);
				jmeasure =  (double)  pxy*Math.log(pyIFx/py)+ px_y*Math.log(p_yIFx/p_y);

				pmiC1[featureIndex] = pxy;
				liftArr[featureIndex] = lift;
				levArr[featureIndex] = leverage;
				jaccArr[featureIndex] = jaccard;
				jmArr[featureIndex] = jmeasure;
				pC1names[featureIndex] = s;
				stats[featureIndex] = lift + " - "+leverage +" - " + jaccard + " - " + jmeasure +" - " + px;
				featureIndex++;
				//System.out.println(denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapD0.get(s) +" - " +y1count);
			}

			for(String s : ruleKeyListD1){
				int uncount = 0, ccount=0;
				if(unclueMapD1.containsKey(s)){
					uncount = unclueMapD1.get(s);
				}
				if(clueMapD1.containsKey(s)){
					ccount = clueMapD1.get(s);
				}
				pxy = ((double) (uncount)/ (y1count+y0count));
				px = ((double) (uncount+ccount)/ (y1count+y0count));
				py = ((double) (y1count)/ (y1count+y0count));
				pyIFx = ((double) (uncount)/ (uncount+ccount));
				p_y = ((double) (y0count)/ (y1count+y0count));
				p_yIFx = ((double) (ccount)/ (uncount+ccount));
				px_y = ((double) (ccount)/ (y1count+y0count));
				lift =(double)  pxy/(px*py);
				leverage =  (double)  pxy-(px*py);
				jaccard =  (double)  pxy/(px+py-pxy);
				jmeasure =  (double)  pxy*Math.log(pyIFx/py)+ px_y*Math.log(p_yIFx/p_y);

				pmiC1[featureIndex] = pxy;
				liftArr[featureIndex] = lift;
				levArr[featureIndex] = leverage;
				jaccArr[featureIndex] = jaccard;
				jmArr[featureIndex] = jmeasure;
				pC1names[featureIndex] = s;
				stats[featureIndex] = lift + " - "+leverage +" - " + jaccard + " - " + jmeasure +" - " + px;
				featureIndex++;
				//System.out.println(denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapD1.get(s) +" - " +y1count);
			}

			for(String s : ruleKeyListDW){
				int uncount = 0, ccount=0;
				if(unclueMapDW.containsKey(s)){
					uncount = unclueMapDW.get(s);
				}
				if(clueMapDW.containsKey(s)){
					ccount = clueMapDW.get(s);
				}
				pxy = ((double) (uncount)/ (y1count+y0count));
				px = ((double) (uncount+ccount)/ (y1count+y0count));
				py = ((double) (y1count)/ (y1count+y0count));
				pyIFx = ((double) (uncount)/ (uncount+ccount));
				p_y = ((double) (y0count)/ (y1count+y0count));
				p_yIFx = ((double) (ccount)/ (uncount+ccount));
				px_y = ((double) (ccount)/ (y1count+y0count));
				lift =(double)  pxy/(px*py);
				leverage =  (double)  pxy-(px*py);
				jaccard =  (double)  pxy/(px+py-pxy);
				jmeasure =  (double)  pxy*Math.log(pyIFx/py)+ px_y*Math.log(p_yIFx/p_y);

				pmiC1[featureIndex] = pxy;
				liftArr[featureIndex] = lift;
				levArr[featureIndex] = leverage;
				jaccArr[featureIndex] = jaccard;
				jmArr[featureIndex] = jmeasure;
				pC1names[featureIndex] = s;
				stats[featureIndex] = lift + " - "+leverage +" - " + jaccard + " - " + jmeasure +" - " + px;
				featureIndex++;
				//System.out.println(denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapDW.get(s) +" - " +y1count);
			}

			for(String s : ruleKeyListDN){
				int uncount = 0, ccount=0;
				if(unclueMapDN.containsKey(s)){
					uncount = unclueMapDN.get(s);
				}
				if(clueMapDN.containsKey(s)){
					ccount = clueMapDN.get(s);
				}
				pxy = ((double) (uncount)/ (y1count+y0count));
				px = ((double) (uncount+ccount)/ (y1count+y0count));
				py = ((double) (y1count)/ (y1count+y0count));
				pyIFx = ((double) (uncount)/ (uncount+ccount));
				p_y = ((double) (y0count)/ (y1count+y0count));
				p_yIFx = ((double) (ccount)/ (uncount+ccount));
				px_y = ((double) (ccount)/ (y1count+y0count));
				lift =(double)  pxy/(px*py);
				leverage =  (double)  pxy-(px*py);
				jaccard =  (double)  pxy/(px+py-pxy);
				jmeasure =  (double)  pxy*Math.log(pyIFx/py)+ px_y*Math.log(p_yIFx/p_y);

				pmiC1[featureIndex] = pxy;
				liftArr[featureIndex] = lift;
				levArr[featureIndex] = leverage;
				jaccArr[featureIndex] = jaccard;
				jmArr[featureIndex] = jmeasure;
				pC1names[featureIndex] = s;

				stats[featureIndex] = lift + " - "+leverage +" - " + jaccard + " - " + jmeasure +" - " + px;
				featureIndex++;
				//System.out.println(denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapDN.get(s) +" - " +y1count);
			}

			ArrayList<Double> top1 = new ArrayList<Double>();
			ArrayList<Integer> top1idx = new ArrayList<Integer>();
			ArrayList<String> top1name = new ArrayList<String>();
			ArrayList<String> top1stats = new ArrayList<String>();

			/*	//while(top1.size()< k){
			while(top1.size()< (y1count+y0count)){
				Double largest = -Double.MAX_VALUE;
				int index = 0;

				for (int i = 0; i < pmiC1.length; i++) {
					if ( pmiC1[i] - largest >0 ) {//TODO check
						largest = pmiC1[i];
						index = i;
					}
				}
				if(pmiC1.length>0)
					pmiC1[index] = -Double.MAX_VALUE;
				top1.add(new Double(largest));
				top1idx.add(new Integer(index));
			}

			//if(pC1names.length>0 && stats.length>0)
			for(int i : top1idx){
				top1name.add(pC1names[i]);
				top1stats.add(stats[i]);
			}
			 */
			BufferedWriter writer = new BufferedWriter (new FileWriter(ff));
			for (int i = 0; i<pC1names.length; i++){
				//	if(top1idx)
				writer.write(pC1names[i] + "\t" + i + "\t" + pmiC1[i]  + "\t" + stats[i] );
				writer.newLine();
			}
			writer.close();


			BufferedWriter writer2 = new BufferedWriter (new FileWriter(clues_rules_out));

			if(liftF!=Double.NEGATIVE_INFINITY){
				if(levF!=Double.NEGATIVE_INFINITY){
					if(jaccF!=Double.NEGATIVE_INFINITY){
						if(jmF!=Double.NEGATIVE_INFINITY){
							write4F(writer2,pC1names,liftArr,levArr,jaccArr,jmArr,liftF,levF,jaccF,jmF);
						}
						else{
							write3F(writer2,pC1names,liftArr,levArr,jaccArr,liftF,levF,jaccF);
						}
					}
					else{
						if(jmF!=Double.NEGATIVE_INFINITY){
							write3F(writer2,pC1names,liftArr,levArr,jmArr,liftF,levF,jmF);
						}
						else{
							write2F(writer2,pC1names,liftArr,levArr,liftF,levF);
						}
					}
				}
				else{
					if(jaccF!=Double.NEGATIVE_INFINITY){
						if(jmF!=Double.NEGATIVE_INFINITY){
							write3F(writer2,pC1names,liftArr,levArr,jmArr,liftF,jaccF,jmF);
						}
						else{
							write2F(writer2,pC1names,liftArr,jaccArr,liftF,jaccF);
						}
					}
					else{
						if(jmF!=Double.NEGATIVE_INFINITY){
							write2F(writer2,pC1names,liftArr,jmArr,liftF,jmF);
						}
						else{
							write1F(writer2,pC1names,liftArr,liftF);
						}
					}
				}
			}
			else{
				if(levF!=Double.NEGATIVE_INFINITY){
					if(jaccF!=Double.NEGATIVE_INFINITY){
						if(jmF!=Double.NEGATIVE_INFINITY){
							write3F(writer2,pC1names,levArr,jaccArr,jmArr,levF,jaccF,jmF);
						}
						else{
							write2F(writer2,pC1names,levArr,jaccArr,levF,jaccF);
						}
					}
					else{
						if(jmF!=Double.NEGATIVE_INFINITY){
							write2F(writer2,pC1names,levArr,jmArr,levF,jmF);
						}
						else{
							write1F(writer2,pC1names,levArr,levF);
						}
					}
				}
				else{
					if(jaccF!=Double.NEGATIVE_INFINITY){
						if(jmF!=Double.NEGATIVE_INFINITY){
							write2F(writer2,pC1names,jaccArr,jmArr,jaccF,jmF);
						}
						else{
							write1F(writer2,pC1names,jaccArr,jaccF);
						}
					}
					else{
						if(jmF!=Double.NEGATIVE_INFINITY){
							write1F(writer2,pC1names,jmArr,jmF);
						}
						else{
							write(writer2,pC1names);
						}
					}
				}
			}

			
		}

		catch (Exception e) {
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}
		super.collectionProcessComplete();
	}


	private void write4F(BufferedWriter writer2, String[] pC1names, double[] arr1,
			double[] arr2, double[] arr3, double[] arr4, double f1, double f2,
			double f3, double f4) throws IOException {
		for (int i = 0; i<pC1names.length; i++){
			if(arr1[i]>f1 && arr2[i]>f2 && arr3[i]>f3 && arr4[i]>f4){
				writer2.write(pC1names[i]);
				writer2.newLine();
			}
		}
		writer2.close();

	}

	private void write3F(BufferedWriter writer2, String[] pC1names, double[] arr1,
			double[] arr2, double[] arr3, double f1, double f2,
			double f3) throws IOException {
		for (int i = 0; i<pC1names.length; i++){
			if(arr1[i]>f1 && arr2[i]>f2 && arr3[i]>f3 ){
				writer2.write(pC1names[i]);
				writer2.newLine();
			}
		}
		writer2.close();

	}

	private void write2F(BufferedWriter writer2, String[] pC1names, double[] arr1,
			double[] arr2, double f1, double f2) throws IOException {
		for (int i = 0; i<pC1names.length; i++){
			if(arr1[i]>f1 && arr2[i]>f2 ){
				writer2.write(pC1names[i]);
				writer2.newLine();
			}
		}
		writer2.close();

	}

	private void write1F(BufferedWriter writer2, String[] pC1names, double[] arr, double f) throws IOException {
		for (int i = 0; i<pC1names.length; i++){
			if(arr[i]>f ){
				writer2.write(pC1names[i]);
				writer2.newLine();
			}
		}
		writer2.close();

	}

	private void write(BufferedWriter writer2, String[] pC1names) throws IOException {
		for (int i = 0; i<pC1names.length; i++){
			
				writer2.write(pC1names[i]);
				writer2.newLine();
			}
		writer2.close();

	}

	private boolean verifyCompatibility() {
		Iterator<String> iterUn0 = unclueMapD0.keySet().iterator();
		for(String s : clueMapD0.keySet()){
			if(iterUn0.hasNext()){
				if(!iterUn0.next().equals(s)){
					System.out.println("Incompatible sets");
					return false;
				}
			}
		}

		Iterator<String> iterUn1 = unclueMapD1.keySet().iterator();
		for(String s : clueMapD1.keySet()){
			if(iterUn1.hasNext()){
				if(!iterUn1.next().equals(s)){
					System.out.println("Incompatible sets");
					return false;
				}
			}
		}
		Iterator<String> iterUnW = unclueMapDW.keySet().iterator();
		for(String s : clueMapDW.keySet()){
			if(iterUnW.hasNext()){
				if(!iterUnW.next().equals(s)){
					System.out.println("Incompatible sets");
					return false;
				}
			}
		}
		Iterator<String> iterUnN = unclueMapDN.keySet().iterator();
		for(String s : clueMapDN.keySet()){
			if(iterUnN.hasNext()){
				if(!iterUnN.next().equals(s)){
					System.out.println("Incompatible sets");
					return false;
				}
			}
		}
		return true;
	}


	private void aolkArg2not(Event event, EnjuSentence sentence, JCas jcas) {
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		EnjuToken previous = null;
		boolean found = false;
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			boolean semHeadIsAnEntity = isAnArgument(currToken, sentence, jcas);
			if(!semHeadIsAnEntity){
				if(previous!=null){
					if(previous.getBase().toLowerCase().equals("no") || previous.getBase().toLowerCase().equals("not")){
						SyntacticAnnotation arg2Node = currToken.getArg2();
						if(!(arg2Node == null)){
							if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){

								int countOccurences = 1;
								String featureString = tString  + "_DN_Arg2";
								if(!event.getSpeculation()){
									if(clueMapDN.containsKey(featureString))
										countOccurences = countOccurences + clueMapDN.get(featureString);
									clueMapDN.put(featureString, countOccurences);
									if(!unclueMapDN.containsKey(featureString))
										unclueMapDN.put(featureString, 0);
								}
								else{
									if(unclueMapDN.containsKey(featureString))
										countOccurences = countOccurences + unclueMapDN.get(featureString);
									unclueMapDN.put(featureString, countOccurences);
									if(!clueMapDN.containsKey(featureString))
										clueMapDN.put(featureString, 0);
								}
								if(!ruleKeyListDN.contains(featureString)){
									ruleKeyListDN.add(featureString);
								}
								rcount++;
								found=true;
							}
						}
						if (!found){
							arg2Node = previous.getArg2();
							if(!(arg2Node == null)){
								if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
									int countOccurences = 1;
									String featureString = tString + "_DN_Arg2";
									if(!event.getSpeculation()){
										if(clueMapDN.containsKey(featureString))
											countOccurences = countOccurences + clueMapDN.get(featureString);
										clueMapDN.put(featureString, countOccurences);
										if(!unclueMapDN.containsKey(featureString))
											unclueMapDN.put(featureString, 0);
									}
									else{
										if(unclueMapDN.containsKey(featureString))
											countOccurences = countOccurences + unclueMapDN.get(featureString);
										unclueMapDN.put(featureString, countOccurences);
										if(!clueMapDN.containsKey(featureString))
											clueMapDN.put(featureString, 0);
									}
									if(!ruleKeyListDN.contains(featureString)){
										ruleKeyListDN.add(featureString);
									}
									rcount++;
								}
							}
						}
					}
				}// if not found
			}
			previous = currToken;
		}//while tokenIterator
	}


	private void aolkArg1not(Event event, EnjuSentence sentence, JCas jcas) {
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		EnjuToken previous = null;
		boolean found = false;
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			boolean semHeadIsAnEntity = isAnArgument(currToken, sentence, jcas);
			if(!semHeadIsAnEntity){
				if(previous!=null){
					if(previous.getBase().toLowerCase().equals("no") || previous.getBase().toLowerCase().equals("not")){
						SyntacticAnnotation arg1Node = currToken.getArg1();
						if(!(arg1Node == null)){
							if(semHeadMatches(jcas, arg1Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString  + "_DN_Arg1";
								if(!event.getSpeculation()){
									if(clueMapDN.containsKey(featureString))
										countOccurences = countOccurences + clueMapDN.get(featureString);
									clueMapDN.put(featureString, countOccurences);
									if(!unclueMapDN.containsKey(featureString))
										unclueMapDN.put(featureString, 0);
								}
								else{
									if(unclueMapDN.containsKey(featureString))
										countOccurences = countOccurences + unclueMapDN.get(featureString);
									unclueMapDN.put(featureString, countOccurences);
									if(!clueMapDN.containsKey(featureString))
										clueMapDN.put(featureString, 0);
								}
								if(!ruleKeyListDN.contains(featureString)){
									ruleKeyListDN.add(featureString);
								}
								rcount++;
								found=true;
							}
						}
						if (!found){
							arg1Node = previous.getArg1();
							if(!(arg1Node == null)){
								if(semHeadMatches(jcas, arg1Node, triggerBegin, triggerEnd)){
									int countOccurences = 1;
									String featureString = tString + "_DN_Arg1";
									if(!event.getSpeculation()){
										if(clueMapDN.containsKey(featureString))
											countOccurences = countOccurences + clueMapDN.get(featureString);
										clueMapDN.put(featureString, countOccurences);
										if(!unclueMapDN.containsKey(featureString))
											unclueMapDN.put(featureString, 0);
									}
									else{
										if(unclueMapDN.containsKey(featureString))
											countOccurences = countOccurences + unclueMapDN.get(featureString);
										unclueMapDN.put(featureString, countOccurences);
										if(!clueMapDN.containsKey(featureString))
											clueMapDN.put(featureString, 0);
									}
									if(!ruleKeyListDN.contains(featureString)){
										ruleKeyListDN.add(featureString);
									}
									rcount++;
								}

							}
						}
					}
				}// if not found
			}
			previous = currToken;
		}//while tokenIterator

	}



	private void DWArg2Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			boolean semHeadIsAnEntity = isAnArgument(currToken, sentence, jcas);
			if(!semHeadIsAnEntity){
				SyntacticAnnotation arg2Node = currToken.getArg2();
				if(!(arg2Node == null)){
					if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
						EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
						EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
						if(semHeadToken != null){
							String clueCandidate = semHeadToken.getBase().toLowerCase();
							semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
							if(!semHeadIsAnEntity){
								SyntacticAnnotation secarg1Node = currToken.getArg1();
								if(!(secarg1Node == null)){
									if(semHeadMatches(jcas, secarg1Node, triggerBegin, triggerEnd)){
										int countOccurences = 1;
										String featureString = tString + "_"+ clueCandidate + "_DW_Arg2_Arg1";
										if(!event.getSpeculation()){
											if(clueMapDW.containsKey(featureString))
												countOccurences = countOccurences + clueMapDW.get(featureString);
											clueMapDW.put(featureString, countOccurences);
											if(!unclueMapDW.containsKey(featureString))
												unclueMapDW.put(featureString, 0);
										}
										else{
											if(unclueMapDW.containsKey(featureString))
												countOccurences = countOccurences + unclueMapDW.get(featureString);
											unclueMapDW.put(featureString, countOccurences);
											if(!clueMapDW.containsKey(featureString))
												clueMapDW.put(featureString, 0);
										}
										if(!ruleKeyListDW.contains(featureString)){
											ruleKeyListDW.add(featureString);
										}
										rcount++;
									}
								}
							}
						}
					}
				}
			}
		}//while tokenIterator

	}


	private void DWArg1Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			boolean semHeadIsAnEntity = isAnArgument(currToken, sentence, jcas);
			if(!semHeadIsAnEntity){

				SyntacticAnnotation arg1Node = currToken.getArg1();
				if(!(arg1Node == null)){
					if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
						EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
						EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
						if(semHeadToken != null){
							String clueCandidate = semHeadToken.getBase().toLowerCase();
							semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
							if(!semHeadIsAnEntity){

								SyntacticAnnotation secarg2Node = currToken.getArg2();
								if(!(secarg2Node == null)){
									if(semHeadMatches(jcas, secarg2Node, triggerBegin, triggerEnd)){
										int countOccurences = 1;
										String featureString = tString + "_"+ clueCandidate + "_DW_Arg1_Arg2";
										if(!event.getSpeculation()){
											if(clueMapDW.containsKey(featureString))
												countOccurences = countOccurences + clueMapDW.get(featureString);
											clueMapDW.put(featureString, countOccurences);
											if(!unclueMapDW.containsKey(featureString))
												unclueMapDW.put(featureString, 0);
										}
										else{
											if(unclueMapDW.containsKey(featureString))
												countOccurences = countOccurences + unclueMapDW.get(featureString);
											unclueMapDW.put(featureString, countOccurences);
											if(!clueMapDW.containsKey(featureString))
												clueMapDW.put(featureString, 0);
										}
										if(!ruleKeyListDW.contains(featureString)){
											ruleKeyListDW.add(featureString);
										}
										rcount++;
									}
								}
							}
						}
					}
				}
			}
		}//while tokenIterator

	}


	private void D1Arg2Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();


			SyntacticAnnotation arg2Node = currToken.getArg2();
			if(!(arg2Node == null)){
				if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
					EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
					if(semHeadToken != null){
						boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
						boolean semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
						if(!semHeadIsATrigger && !semHeadIsAnEntity){
							String middleWordLemma = semHeadToken.getBase();
							SyntacticAnnotation secondArg2Node = semHeadToken.getArg2();
							if(semHeadMatches(jcas, secondArg2Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString + "_"+ middleWordLemma + "_D1_Arg2_Arg2";
								if(!event.getSpeculation()){
									if(clueMapD1.containsKey(featureString))
										countOccurences = countOccurences + clueMapD1.get(featureString);
									clueMapD1.put(featureString, countOccurences);
									if(!unclueMapD1.containsKey(featureString))
										unclueMapD1.put(featureString, 0);
								}
								else{
									if(unclueMapD1.containsKey(featureString))
										countOccurences = countOccurences + unclueMapD1.get(featureString);
									unclueMapD1.put(featureString, countOccurences);
									if(!clueMapD1.containsKey(featureString))
										clueMapD1.put(featureString, 0);
								}
								if(!ruleKeyListD1.contains(featureString)){
									ruleKeyListD1.add(featureString);
								}
								rcount++;
							}
						}//if semHead is not a trigger
					}
				}
			}

		}//while tokenIterator

	}


	private void D1Arg1Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();

			SyntacticAnnotation arg1Node = currToken.getArg1();
			if(!(arg1Node == null)){
				if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
					EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
					if(semHeadToken != null){
						boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
						boolean semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
						if(!semHeadIsATrigger && !semHeadIsAnEntity){
							String middleWordLemma = semHeadToken.getBase();
							SyntacticAnnotation secondArg2Node = semHeadToken.getArg2();
							if(semHeadMatches(jcas, secondArg2Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString + "_"+ middleWordLemma + "_D1_Arg1_Arg2";
								if(featureString.contains("+")){
									System.out.println(featureString);
								}
								if(!event.getSpeculation()){
									if(clueMapD1.containsKey(featureString))
										countOccurences = countOccurences + clueMapD1.get(featureString);
									clueMapD1.put(featureString, countOccurences);
									if(!unclueMapD1.containsKey(featureString))
										unclueMapD1.put(featureString, 0);
								}
								else{
									if(unclueMapD1.containsKey(featureString))
										countOccurences = countOccurences + unclueMapD1.get(featureString);
									unclueMapD1.put(featureString, countOccurences);
									if(!clueMapD1.containsKey(featureString))
										clueMapD1.put(featureString, 0);
								}
								if(!ruleKeyListD1.contains(featureString)){
									ruleKeyListD1.add(featureString);
								}
								rcount++;
							}
						}//if semHead is not a trigger
					}
				}
			}

		}//while tokenIterator

	}


	private void D1Arg2Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();

			SyntacticAnnotation arg2Node = currToken.getArg2();
			if(!(arg2Node == null)){
				if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
					EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
					if(semHeadToken != null){
						boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
						boolean semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
						if(!semHeadIsATrigger && !semHeadIsAnEntity){
							String middleWordLemma = semHeadToken.getBase();
							SyntacticAnnotation secondArg1Node = semHeadToken.getArg1();
							if(semHeadMatches(jcas, secondArg1Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString + "_"+ middleWordLemma + "_D1_Arg2_Arg1";
								if(!event.getSpeculation()){
									if(clueMapD1.containsKey(featureString))
										countOccurences = countOccurences + clueMapD1.get(featureString);
									clueMapD1.put(featureString, countOccurences);
									if(!unclueMapD1.containsKey(featureString))
										unclueMapD1.put(featureString, 0);
								}
								else{
									if(unclueMapD1.containsKey(featureString))
										countOccurences = countOccurences + unclueMapD1.get(featureString);
									unclueMapD1.put(featureString, countOccurences);
									if(!clueMapD1.containsKey(featureString))
										clueMapD1.put(featureString, 0);
								}
								if(!ruleKeyListD1.contains(featureString)){
									ruleKeyListD1.add(featureString);
								}
								rcount++;
							}
						}//if semHead is not a trigger
					}
				}
			}

		}//while tokenIterator

	}


	private void D1Arg1Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();


			SyntacticAnnotation arg1Node = currToken.getArg1();
			if(!(arg1Node == null)){
				if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg2NodeEC = (EnjuConstituent) arg1Node;
					EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
					if(semHeadToken != null){
						boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
						boolean semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
						if(!semHeadIsATrigger && !semHeadIsAnEntity){
							String middleWordLemma = semHeadToken.getBase();
							SyntacticAnnotation secondArg1Node = semHeadToken.getArg1();
							if(semHeadMatches(jcas, secondArg1Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString + "_"+ middleWordLemma + "_D1_Arg1_Arg1";
								if(!event.getSpeculation()){
									if(clueMapD1.containsKey(featureString))
										countOccurences = countOccurences + clueMapD1.get(featureString);
									clueMapD1.put(featureString, countOccurences);
									if(!unclueMapD1.containsKey(featureString))
										unclueMapD1.put(featureString, 0);
								}
								else{
									if(unclueMapD1.containsKey(featureString))
										countOccurences = countOccurences + unclueMapD1.get(featureString);
									unclueMapD1.put(featureString, countOccurences);
									if(!clueMapD1.containsKey(featureString))
										clueMapD1.put(featureString, 0);
								}
								if(!ruleKeyListD1.contains(featureString)){
									ruleKeyListD1.add(featureString);
								}rcount++;
								rcount++;
							}
						}//if semHead is not a trigger
					}
				}
			}

		}//while tokenIterator
	}





	private void D1Arg2Arg2(Event event, EnjuSentence sentence, JCas jcas, String cue) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			SyntacticAnnotation arg2Node = currToken.getArg2();
			if(!(arg2Node == null)){
				if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
					EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
					if(semHeadToken != null){
						boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
						boolean semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
						if(!semHeadIsATrigger && !semHeadIsAnEntity){
							String middleWordLemma = semHeadToken.getBase();
							SyntacticAnnotation secondArg2Node = semHeadToken.getArg2();
							if(semHeadMatches(jcas, secondArg2Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString + "_"+ middleWordLemma + "_D1_Arg2_Arg2";
								if(!event.getSpeculation()){
									if(clueMapD1.containsKey(featureString))
										countOccurences = countOccurences + clueMapD1.get(featureString);
									clueMapD1.put(featureString, countOccurences);
									if(!unclueMapD1.containsKey(featureString))
										unclueMapD1.put(featureString, 0);
								}
								else{
									if(unclueMapD1.containsKey(featureString))
										countOccurences = countOccurences + unclueMapD1.get(featureString);
									unclueMapD1.put(featureString, countOccurences);
									if(!clueMapD1.containsKey(featureString))
										clueMapD1.put(featureString, 0);
								}
								if(!ruleKeyListD1.contains(featureString)){
									ruleKeyListD1.add(featureString);
								}rcount++;
								rcount++;
							}
						}//if semHead is not a trigger
					}
				}
			}
		}//while tokenIterator


	}

	private void D1Arg2Arg1(Event event, EnjuSentence sentence, JCas jcas, String cue) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			SyntacticAnnotation arg2Node = currToken.getArg2();
			if(!(arg2Node == null)){
				if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
					EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
					if(semHeadToken != null){
						boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
						boolean semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
						if(!semHeadIsATrigger && !semHeadIsAnEntity){
							String middleWordLemma = semHeadToken.getBase();
							SyntacticAnnotation secondArg1Node = semHeadToken.getArg1();
							if(semHeadMatches(jcas, secondArg1Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString + "_"+ middleWordLemma + "_D1_Arg2_Arg1";
								if(!event.getSpeculation()){
									if(clueMapD1.containsKey(featureString))
										countOccurences = countOccurences + clueMapD1.get(featureString);
									clueMapD1.put(featureString, countOccurences);
									if(!unclueMapD1.containsKey(featureString))
										unclueMapD1.put(featureString, 0);
								}
								else{
									if(unclueMapD1.containsKey(featureString))
										countOccurences = countOccurences + unclueMapD1.get(featureString);
									unclueMapD1.put(featureString, countOccurences);
									if(!clueMapD1.containsKey(featureString))
										clueMapD1.put(featureString, 0);
								}
								if(!ruleKeyListD1.contains(featureString)){
									ruleKeyListD1.add(featureString);
								}rcount++;
								rcount++;
							}
						}//if semHead is not a trigger
					}
				}
			}

		}//while tokenIterator


	}

	private void D1Arg1Arg2(Event event, EnjuSentence sentence, JCas jcas, String cue) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			SyntacticAnnotation arg1Node = currToken.getArg1();
			if(!(arg1Node == null)){
				if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
					EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
					if(semHeadToken != null){
						boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
						boolean semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
						if(!semHeadIsATrigger && !semHeadIsAnEntity){
							String middleWordLemma = semHeadToken.getBase();
							SyntacticAnnotation secondArg2Node = semHeadToken.getArg2();
							if(semHeadMatches(jcas, secondArg2Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString + "_"+ middleWordLemma + "_D1_Arg1_Arg2";
								if(featureString.contains("+")){
									System.out.println(featureString);
								}
								if(!event.getSpeculation()){
									if(clueMapD1.containsKey(featureString))
										countOccurences = countOccurences + clueMapD1.get(featureString);
									clueMapD1.put(featureString, countOccurences);
									if(!unclueMapD1.containsKey(featureString))
										unclueMapD1.put(featureString, 0);
								}
								else{
									if(unclueMapD1.containsKey(featureString))
										countOccurences = countOccurences + unclueMapD1.get(featureString);
									unclueMapD1.put(featureString, countOccurences);
									if(!clueMapD1.containsKey(featureString))
										clueMapD1.put(featureString, 0);
								}
								if(!ruleKeyListD1.contains(featureString)){
									ruleKeyListD1.add(featureString);
								}rcount++;
								rcount++;
							}
						}//if semHead is not a trigger
					}
				}
			}
		}//while tokenIterator


	}

	private void D1Arg1Arg1(Event event, EnjuSentence sentence, JCas jcas, String cue) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			SyntacticAnnotation arg1Node = currToken.getArg1();
			if(!(arg1Node == null)){
				if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
					EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
					if(semHeadToken != null){
						boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
						boolean semHeadIsAnEntity = isAnArgument(semHeadToken, sentence, jcas);
						if(!semHeadIsATrigger && !semHeadIsAnEntity){
							String middleWordLemma = semHeadToken.getBase();
							SyntacticAnnotation secondArg1Node = semHeadToken.getArg1();
							if(semHeadMatches(jcas, secondArg1Node, triggerBegin, triggerEnd)){
								int countOccurences = 1;
								String featureString = tString + "_"+ middleWordLemma + "_D1_Arg1_Arg1";
								if(!event.getSpeculation()){
									if(clueMapD1.containsKey(featureString))
										countOccurences = countOccurences + clueMapD1.get(featureString);
									clueMapD1.put(featureString, countOccurences);
									if(!unclueMapD1.containsKey(featureString))
										unclueMapD1.put(featureString, 0);
								}
								else{
									if(unclueMapD1.containsKey(featureString))
										countOccurences = countOccurences + unclueMapD1.get(featureString);
									unclueMapD1.put(featureString, countOccurences);
									if(!clueMapD1.containsKey(featureString))
										clueMapD1.put(featureString, 0);
								}
								if(!ruleKeyListD1.contains(featureString)){
									ruleKeyListD1.add(featureString);
								}rcount++;
								rcount++;
							}
						}//if semHead is not a trigger
					}
				}
			}
		}//while tokenIterator

	}

	private void D0Arg1MW(Event event, EnjuSentence sentence, JCas jcas) {

		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);

		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			boolean semHeadIsAnEntity = isAnArgument(currToken, sentence, jcas);
			if(!semHeadIsAnEntity){

				String tString = currToken.getBase();
				SyntacticAnnotation arg1Node = currToken.getArg1();
				if(!(arg1Node == null)){
					if(semHeadMatches(jcas, arg1Node, triggerBegin, triggerEnd)){
						int countOccurences = 1;
						String featureString = tString + "_D0_Arg1" ;
						if(!event.getSpeculation()){ //if event is certain
							if(clueMapD0.containsKey(featureString))
								countOccurences = countOccurences + clueMapD0.get(featureString);
							clueMapD0.put(featureString, countOccurences);//increase occurrence in certain events
							if(!unclueMapD0.containsKey(featureString))//if not in uncertain map put with 0 occurrences
								unclueMapD0.put(featureString, 0);
						}
						else{
							if(unclueMapD0.containsKey(featureString))//if event is uncertain
								countOccurences = countOccurences + unclueMapD0.get(featureString);
							unclueMapD0.put(featureString, countOccurences);//increase occurrence in uncertain events
							if(!clueMapD0.containsKey(featureString))//if not in certain map put with 0 occurrences
								clueMapD0.put(featureString, 0);
						}
						if(!ruleKeyListD0.contains(featureString)){//add new rule
							ruleKeyListD0.add(featureString);
						}
						rcount++;
					}
				}
			}
		}
	}


	private void D0Arg2MW(Event event, EnjuSentence sentence, JCas jcas) {

		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);

		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			boolean semHeadIsAnEntity = isAnArgument(currToken, sentence, jcas);
			if(!semHeadIsAnEntity){

				String tString = currToken.getBase();
				SyntacticAnnotation arg2Node = currToken.getArg2();
				if(!(arg2Node == null)){
					if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
						int countOccurences = 1;
						String featureString = tString + "_D0_Arg2" ;
						if(!event.getSpeculation()){ //if event is certain
							if(clueMapD0.containsKey(featureString))
								countOccurences = countOccurences + clueMapD0.get(featureString);
							clueMapD0.put(featureString, countOccurences);//increase occurrence in certain events
							if(!unclueMapD0.containsKey(featureString))//if not in uncertain map put with 0 occurrences
								unclueMapD0.put(featureString, 0);
						}
						else{
							if(unclueMapD0.containsKey(featureString))//if event is uncertain
								countOccurences = countOccurences + unclueMapD0.get(featureString);
							unclueMapD0.put(featureString, countOccurences);//increase occurrence in uncertain events
							if(!clueMapD0.containsKey(featureString))//if not in certain map put with 0 occurrences
								clueMapD0.put(featureString, 0);
						}
						if(!ruleKeyListD0.contains(featureString)){//add new rule
							ruleKeyListD0.add(featureString);
						}
						rcount++;
					}
				}
			}

		}
	}



	//Helper Method: Follows the route of semantic head constituents and returns the final semantic head (EnjuToken) for the given EnjuConstituent
	private EnjuToken getSemHeadToken(EnjuConstituent ec, JCas jcas){
		EnjuToken semHeadToken = new EnjuToken(jcas);
		Annotation semHead = ec.getSemHead();
		if(semHead!=null){
			if(semHead.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuToken")){
				semHeadToken = (EnjuToken) semHead;
			}
			else{
				EnjuConstituent semHeadEC = (EnjuConstituent) semHead;
				semHeadToken = getSemHeadToken(semHeadEC, jcas);
			}
			return semHeadToken;
		}
		return null;

	}

	//Helper Method: Checks if a given token is a trigger
	private boolean isATrigger(EnjuToken token, FSIterator<Annotation> eventIterator){
		boolean result = false;
		while (eventIterator.hasNext()) {
			Event currEvent = (Event) eventIterator.next();
			int b1 = token.getBegin();
			int b2 = currEvent.getBegin();
			int e1 = token.getEnd();
			int e2 = currEvent.getEnd();
			if(b1==b2 && e1==e2){
				result = true;
				break;
			}
		}
		return result;
	}

	private boolean isAnArgument(EnjuToken token, EnjuSentence sentence, JCas jcas){
		boolean result = false;
		FSIterator<Annotation> entityIterator = jcas.getAnnotationIndex(Entity.type).subiterator(sentence);
		while (entityIterator.hasNext()) {
			Entity currEntity = (Entity) entityIterator.next();
			int b1 = token.getBegin();
			int b2 = currEntity.getBegin();
			int e1 = token.getEnd();
			int e2 = currEntity.getEnd();
			if(b1==b2 && e1==e2){
				result = true;
				break;
			}
		}
		return result;
	}

	//Helper Method: Checks if the given node has a semantic head with given begin and end values
	private boolean semHeadMatches(JCas jcas, SyntacticAnnotation someNode, int begin, int end){
		boolean result = false;
		if(!(someNode == null)){
			if(someNode.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
				EnjuConstituent modNodeEC = (EnjuConstituent) someNode;
				EnjuToken smeHeadToken = getSemHeadToken(modNodeEC, jcas);
				if(smeHeadToken == null){
					return false;
				}
				if((smeHeadToken.getBegin() <= begin) && (smeHeadToken.getEnd() >= end)){
					result = true;
				}
			}
		}
		return result;
	}




}

