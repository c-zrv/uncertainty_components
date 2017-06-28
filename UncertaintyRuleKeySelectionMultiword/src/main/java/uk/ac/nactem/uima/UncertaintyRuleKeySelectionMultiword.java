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

import uk.ac.nactem.uima.cas.bionlpst.Event;




public class UncertaintyRuleKeySelectionMultiword extends JCasAnnotator_ImplBase {

	private ArrayList<String> clueList;
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
	public static final String PARAM_NAME_CUEFILE = "CueFile"; //file to read rules from
	public static final String PARAM_NAME_PMIFILE = "PMIFile"; //file to read rules from
	private int k;
	private File ff, clues_rules_out;

	int y1count, y0count, rcount;
	private Double pmiScore = -10.0;
	private HashMap<String, List<String>> clueListInit;



	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		try {
			clueList = intitialiseClueList(new File((String)(aContext.getConfigParameterValue(PARAM_NAME_CUEFILE))));
		/*	BufferedWriter writer = new BufferedWriter(new FileWriter("resources/testClues4.txt"));
			for(String cue: clueList){
				writer.write(cue);
				writer.newLine();
			}
			writer.close();*/
			ruleKeyListD0 = new ArrayList<String>();
			ruleKeyListD1 = new ArrayList<String>();
			ruleKeyListDW = new ArrayList<String>();
			ruleKeyListDN = new ArrayList<String>();

			k = 500;
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
				D1Arg1Arg1MW(event, currSentence, jcas);
				D1Arg1Arg2MW(event, currSentence, jcas);
				D1Arg2Arg1MW(event, currSentence, jcas);
				D1Arg2Arg2MW(event, currSentence, jcas);
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
		//verification

		if(!verifyCompatibility()){
			System.out.println("WARNING: Incompatible maps. "); //not needed any more
		}
		System.out.println("HERE");
		try{

			String [] pC1names = new String [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];
			String [] stats = new String [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];


			double[] pmiC1  = new double [clueMapD0.keySet().size()+ clueMapD1.keySet().size()+ clueMapDW.keySet().size() + clueMapDN.keySet().size()];
			int  featureIndex =0;
			for(String s : ruleKeyListD0){
				if(s.equals("able_D0_Arg2")){
					System.out.println("going over able");
				}
				int uncount = 0, ccount = 0;
				if(unclueMapD0.containsKey(s)){
					uncount = unclueMapD0.get(s);
				}

				if(clueMapD0.containsKey(s)){
					ccount = clueMapD0.get(s);
				}
				double denominator1 = ((double)(uncount+ccount)/rcount);//((double)(uncount+ccount)*(double)(y1count)) ; //probability of rule occurence 
				double numerator1 = ((double)uncount/(uncount+ccount));//((double)uncount*(double)(rcount)); //probability of rule  occurence in uncertain events
				//		double denominator1 = ((double)(uncount+ccount)*(double)(y1count)) ; //probability of rule occurence * probability of an event being uncertain
				//		double numerator1 = ((double)uncount); //probability of rule  being uncertain
				double val1 = Math.log(numerator1/denominator1);
				//				double val1nl = Math.log(numerator1/denominator1);
				if(Double.isNaN(val1)){
					//        System.out.println("NAN : "+ val1);
					val1 = -1.0;
				}
				double val2 = val1*uncount/rcount;
				val2 = ((double)uncount/(uncount+ccount))/((double)(uncount+ccount)/rcount);
				val2 = (double)((uncount+ccount)*(1-y1count))/(ccount);
				if(Double.isNaN(val2)){
					//        System.out.println("NAN : "+ val1);
					val2 = -100.0;
				}
				pmiC1[featureIndex] = val2;
				pC1names[featureIndex] = s;
				stats[featureIndex] = denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapD0.get(s) +" - " +y1count;
				featureIndex++;
				System.out.println(denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapD0.get(s) +" - " +y1count);
			}

			for(String s : ruleKeyListD1){
				int uncount = 0, ccount=0;
				if(unclueMapD1.containsKey(s)){
					uncount = unclueMapD1.get(s);
				}
				if(clueMapD1.containsKey(s)){
					ccount = clueMapD1.get(s);
				}
				double denominator1 = ((double)(uncount+ccount)/rcount);//((double)(uncount+ccount)*(double)(y1count)) ; //probability of rule occurence 
				double numerator1 = ((double)uncount/(uncount+ccount));//((double)uncount*(double)(rcount)); //probability of rule  occurence in uncertain events
				//		double denominator1 = ((double)(uncount+ccount)*(double)(y1count)) ; //probability of rule occurence * probability of an event being uncertain
				//		double numerator1 = ((double)uncount); //probability of rule  being uncertain
				double val1 = Math.log(numerator1/denominator1);
				//				double val1nl = Math.log(numerator1/denominator1);
				if(Double.isNaN(val1)){
					//        System.out.println("NAN : "+ val1);
					val1 = -1.0;
				}
				double val2 = val1*uncount/rcount;
				val2 = ((double)uncount/(uncount+ccount))/((double)(uncount+ccount)/rcount);
				val2 = (double)((uncount+ccount)*(1-y1count))/(ccount);
				if(Double.isNaN(val2)){
					//        System.out.println("NAN : "+ val1);
					val2 = -100.0;
				}
				pmiC1[featureIndex] = val2;
				pC1names[featureIndex] = s;
				stats[featureIndex] = denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapD1.get(s) +" - " + y1count;
				featureIndex++;
				System.out.println(denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapD1.get(s) +" - " +y1count);
			}

			for(String s : ruleKeyListDW){
				int uncount = 0, ccount=0;
				if(unclueMapDW.containsKey(s)){
					uncount = unclueMapDW.get(s);
				}
				if(clueMapDW.containsKey(s)){
					ccount = clueMapDW.get(s);
				}
				double denominator1 = ((double)(uncount+ccount)/rcount);//((double)(uncount+ccount)*(double)(y1count)) ; //probability of rule occurence 
				double numerator1 = ((double)uncount/(uncount+ccount));//((double)uncount*(double)(rcount)); //probability of rule  occurence in uncertain events
				//		double denominator1 = ((double)(uncount+ccount)*(double)(y1count)) ; //probability of rule occurence * probability of an event being uncertain
				//		double numerator1 = ((double)uncount); //probability of rule  being uncertain
				double val1 = Math.log(numerator1/denominator1);
				//				double val1nl = Math.log(numerator1/denominator1);
				if(Double.isNaN(val1)){
					//        System.out.println("NAN : "+ val1);
					val1 = -1.0;
				}
				double val2 = val1*uncount/rcount;
				val2 = ((double)uncount/(uncount+ccount))/((double)(uncount+ccount)/rcount);
				val2 = (double)((uncount+ccount)*(1-y1count))/(ccount);
				if(Double.isNaN(val2)){
					//        System.out.println("NAN : "+ val1);
					val2 = -100.0;
				}
				pmiC1[featureIndex] = val2;
				pC1names[featureIndex] = s;
				stats[featureIndex] = denominator1 + " - "+numerator1 +" - " + uncount + " - " +clueMapDW.get(s) +" - " +  y1count;
				featureIndex++;
				System.out.println(denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapDW.get(s) +" - " +y1count);
			}

			for(String s : ruleKeyListDN){
				int uncount = 0, ccount=0;
				if(unclueMapDN.containsKey(s)){
					uncount = unclueMapDN.get(s);
				}
				if(clueMapDN.containsKey(s)){
					ccount = clueMapDN.get(s);
				}
				double denominator1 = ((double)(uncount+ccount)/rcount);//((double)(uncount+ccount)*(double)(y1count)) ; //probability of rule occurence 
				double numerator1 = ((double)uncount/(uncount+ccount));//((double)uncount*(double)(rcount)); //probability of rule  occurence in uncertain events
				//		double denominator1 = ((double)(uncount+ccount)*(double)(y1count)) ; //probability of rule occurence * probability of an event being uncertain
				//		double numerator1 = ((double)uncount); //probability of rule  being uncertain
				double val1 = Math.log(numerator1/denominator1);
				//				double val1nl = Math.log(numerator1/denominator1);
				if(Double.isNaN(val1)){
					//        System.out.println("NAN : "+ val1);
					val1 = -1.0;
				}
				double val2 = val1*uncount/rcount;
				val2 = ((double)uncount/(uncount+ccount))/((double)(uncount+ccount)/rcount);
				val2 = (double)((uncount+ccount)*(1-y1count))/(ccount);
				if(Double.isNaN(val2)){
					//        System.out.println("NAN : "+ val1);
					val2 = -100.0;
				}
				pmiC1[featureIndex] = val2;
				pC1names[featureIndex] = s;
				stats[featureIndex] = denominator1 + " - "+numerator1 +" - " + uncount + " - " +  clueMapDN.get(s)+ " - " + y1count;
				featureIndex++;
				System.out.println(denominator1 + " - "+numerator1 +" - " + uncount + " - " + clueMapDN.get(s) +" - " +y1count);
			}

			ArrayList<Double> top1 = new ArrayList<Double>();
			ArrayList<Integer> top1idx = new ArrayList<Integer>();
			ArrayList<String> top1name = new ArrayList<String>();
			ArrayList<String> top1stats = new ArrayList<String>();

			while(top1.size()< k){
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

			BufferedWriter writer = new BufferedWriter (new FileWriter(ff));
			for (int i = 0; i<k; i++){
			//	if(top1idx)
				writer.write(top1name.get(i) + "\t" + top1idx.get(i) + "\t" +top1.get(i) + "\t" + top1stats.get(i) );
				writer.newLine();
			}
			writer.close();


			BufferedWriter writer2 = new BufferedWriter (new FileWriter(clues_rules_out));
			for (int i = 0; i<k; i++){
			//	if(top1.get(i) > pmiScore){
					writer2.write(top1name.get(i));
					writer2.newLine();
				//}
			}
			writer2.close();
		}

		catch (Exception e) {
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}
		super.collectionProcessComplete();
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
			if(clueList.contains(tString)){
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
			}//if clue found
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
			if(clueList.contains(tString)){
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
			}//if clue found
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

			SyntacticAnnotation arg2Node = currToken.getArg2();
			if(!(arg2Node == null)){
				if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
					EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
					EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
					if(semHeadToken != null){
						String clueCandidate = semHeadToken.getBase().toLowerCase();
						if (clueList.contains(clueCandidate)){
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

			}// if "of" found
		}//while tokenIterator

	}


	private void DWArg1Arg2(Event event, EnjuSentence sentence, JCas jcas) {
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
						String clueCandidate = semHeadToken.getBase().toLowerCase();
						if (clueList.contains(clueCandidate)){
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

			}// if of found
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

			if(clueList.contains(tString)){
				SyntacticAnnotation arg2Node = currToken.getArg2();
				if(!(arg2Node == null)){
					if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
						EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
						EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
						if(semHeadToken != null){
							boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
							if(!semHeadIsATrigger){
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
			}// if clue found
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

			if(clueList.contains(tString)){
				SyntacticAnnotation arg1Node = currToken.getArg1();
				if(!(arg1Node == null)){
					if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
						EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
						EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
						if(semHeadToken != null){
							boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
							if(!semHeadIsATrigger){
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
			}// if clue found
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

			if(clueList.contains(tString)){
				SyntacticAnnotation arg2Node = currToken.getArg2();
				if(!(arg2Node == null)){
					if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
						EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
						EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
						if(semHeadToken != null){
							boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
							if(!semHeadIsATrigger){
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
			}// if clue found
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

			if(clueList.contains(tString)){
				SyntacticAnnotation arg1Node = currToken.getArg1();
				if(!(arg1Node == null)){
					if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
						EnjuConstituent arg2NodeEC = (EnjuConstituent) arg1Node;
						EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
						if(semHeadToken != null){
							boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
							if(!semHeadIsATrigger){
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
			}// if clue found
		}//while tokenIterator
	}


	private void D0Arg2(Event event, EnjuSentence sentence, JCas jcas) {

		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			if(clueList.contains(tString)){
				if(tString.equals("able")){
					System.out.println(sentence.getCoveredText() + " - " + event.getCoveredText());
				}
				SyntacticAnnotation arg2Node = currToken.getArg2();
				if(!(arg2Node == null)){
					if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
						int countOccurences = 1;
						String featureString = tString + "_D0_Arg2" ;
						if(!event.getSpeculation()){
							if(clueMapD0.containsKey(featureString))
								countOccurences = countOccurences + clueMapD0.get(featureString);
							clueMapD0.put(featureString, countOccurences);
							if(!unclueMapD0.containsKey(featureString))
								unclueMapD0.put(featureString, 0);
						}
						else{
							if(unclueMapD0.containsKey(featureString))
								countOccurences = countOccurences + unclueMapD0.get(featureString);
							unclueMapD0.put(featureString, countOccurences);
							if(!clueMapD0.containsKey(featureString))
								clueMapD0.put(featureString, 0);
						}
						if(!ruleKeyListD0.contains(featureString)){
							ruleKeyListD0.add(featureString);
						}
						rcount++;
					}
				}
			}
		}//while tokenIterator

	}


	private void D0Arg1(Event event, EnjuSentence sentence, JCas jcas) {

		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			if(clueList.contains(tString)){
				SyntacticAnnotation arg1Node = currToken.getArg1();
				if(!(arg1Node == null)){
					if(semHeadMatches(jcas, arg1Node, triggerBegin, triggerEnd)){
						int countOccurences = 1;
						String featureString = tString + "_D0_Arg1" ;
						if(!event.getSpeculation()){
							if(clueMapD0.containsKey(featureString))
								countOccurences = countOccurences + clueMapD0.get(featureString);
							clueMapD0.put(featureString, countOccurences);
							if(!unclueMapD0.containsKey(featureString))
								unclueMapD0.put(featureString, 0);
						}
						else{
							if(unclueMapD0.containsKey(featureString))
								countOccurences = countOccurences + unclueMapD0.get(featureString);
							unclueMapD0.put(featureString, countOccurences);
							if(!clueMapD0.containsKey(featureString))
								clueMapD0.put(featureString, 0);
						}
						if(!ruleKeyListD0.contains(featureString)){
							ruleKeyListD0.add(featureString);
						}
						rcount++;
					}
				}
			}
		}//while tokenIterator


	}

	private void D1Arg1Arg1MW(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();



		String sentenceText = sentence.getCoveredText();
		sentenceText = lemmatised(sentenceText);
		List<String> cuesToTest = new ArrayList<String>();
		for(String cue : clueList){
			if(sentenceText.toLowerCase().contains(cue.toLowerCase())){
				cuesToTest.add(cue.toLowerCase());
			}
		}
		if(!cuesToTest.isEmpty()){
			for(String cue : cuesToTest){
				String [] words = cue.split(" ");
				if(words.length==1){
					D1Arg1Arg1(event, sentence, jcas, cue);
				}
				else{

					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
					while (tokenIterator.hasNext()){
						EnjuToken currToken = (EnjuToken) tokenIterator.next();
						String tString = currToken.getBase();

						if(cue.startsWith(tString.toLowerCase())){
							int l = words.length;
							FSIterator<Annotation> tokenIteratorInner = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
							boolean stop = false, start=false;
							int count = 0;
							List<EnjuToken> tokens = new ArrayList<EnjuToken>();
							while(tokenIteratorInner.hasNext() && !stop){
								EnjuToken currTokenInner = (EnjuToken) tokenIteratorInner.next();
								if(!start && currTokenInner.getBase().equals(currToken.getBase()) && currTokenInner.getBegin()==currToken.getBegin()
										&& currTokenInner.getEnd()==currToken.getEnd()){
									start = true;
									tokens.add(currTokenInner);
									count++;
								}
								if(start && words[count].toLowerCase().equals(currTokenInner.getBase().toLowerCase())){
									tokens.add(currTokenInner);
									count++;
									if(count>=l){
										stop=true;
									}
								}
								else if(start){
									start = false;
									count = 0;
									tokens = new ArrayList<EnjuToken>();
								}
							}

							for(EnjuToken currTokenL : tokens){


								SyntacticAnnotation arg1Node = currTokenL.getArg1();
								if(!(arg1Node == null)){
									if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
										EnjuConstituent arg2NodeEC = (EnjuConstituent) arg1Node;
										EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
										if(semHeadToken != null){
											boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
											if(!semHeadIsATrigger){
												String middleWordLemma = semHeadToken.getBase();
												SyntacticAnnotation secondArg1Node = semHeadToken.getArg1();
												if(semHeadMatches(jcas, secondArg1Node, triggerBegin, triggerEnd)){
													int countOccurences = 1;
													String featureString = cue.replaceAll(" ", "_")+"+"+tString + "_"+ middleWordLemma + "_D1_Arg1_Arg1";
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
							}
						}// if clue found
					}//while tokenIterator
				}
			}
		}
	}

	private void D1Arg1Arg2MW(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();



		String sentenceText = sentence.getCoveredText();
		sentenceText = lemmatised(sentenceText);
		List<String> cuesToTest = new ArrayList<String>();
		for(String cue : clueList){
			if(sentenceText.toLowerCase().contains(cue.toLowerCase())){
				cuesToTest.add(cue.toLowerCase());
			}
		}
		if(!cuesToTest.isEmpty()){
			for(String cue : cuesToTest){
				String [] words = cue.split(" ");
				if(words.length==1){
					D1Arg1Arg2(event, sentence, jcas, cue);
				}
				else{

					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
					while (tokenIterator.hasNext()){
						EnjuToken currToken = (EnjuToken) tokenIterator.next();
						String tString = currToken.getBase();

						if(cue.startsWith(tString.toLowerCase())){
							int l = words.length;
							FSIterator<Annotation> tokenIteratorInner = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
							boolean stop = false, start=false;
							int count = 0;
							List<EnjuToken> tokens = new ArrayList<EnjuToken>();
							while(tokenIteratorInner.hasNext() && !stop){
								EnjuToken currTokenInner = (EnjuToken) tokenIteratorInner.next();
								if(!start && currTokenInner.getBase().equals(currToken.getBase()) && currTokenInner.getBegin()==currToken.getBegin()
										&& currTokenInner.getEnd()==currToken.getEnd()){
									start = true;
									tokens.add(currTokenInner);
									count++;
								}
								if(start && words[count].toLowerCase().equals(currTokenInner.getBase().toLowerCase())){
									tokens.add(currTokenInner);
									count++;
									if(count>=l){
										stop=true;
									}
								}
								else if(start){
									start = false;
									count = 0;
									tokens = new ArrayList<EnjuToken>();
								}
							}

							for(EnjuToken currTokenL : tokens){


								SyntacticAnnotation arg1Node = currTokenL.getArg1();
								if(!(arg1Node == null)){
									if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
										EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
										EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
										if(semHeadToken != null){
											boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
											if(!semHeadIsATrigger){
												String middleWordLemma = semHeadToken.getBase();
												SyntacticAnnotation secondArg2Node = semHeadToken.getArg2();
												if(semHeadMatches(jcas, secondArg2Node, triggerBegin, triggerEnd)){
													int countOccurences = 1;
													String featureString = cue.replaceAll(" ", "_")+"+"+tString + "_"+ middleWordLemma + "_D1_Arg1_Arg2";
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
							}
						}// if clue found
					}//while tokenIterator
				}
			}
		}
	}
	
	
	private void D1Arg2Arg1MW(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();



		String sentenceText = sentence.getCoveredText();
		sentenceText = lemmatised(sentenceText);
		List<String> cuesToTest = new ArrayList<String>();
		for(String cue : clueList){
			if(sentenceText.toLowerCase().contains(cue.toLowerCase())){
				cuesToTest.add(cue.toLowerCase());
			}
		}
		if(!cuesToTest.isEmpty()){
			for(String cue : cuesToTest){
				String [] words = cue.split(" ");
				if(words.length==1){
					D1Arg2Arg1(event, sentence, jcas, cue);
				}
				else{

					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
					while (tokenIterator.hasNext()){
						EnjuToken currToken = (EnjuToken) tokenIterator.next();
						String tString = currToken.getBase();

						if(cue.startsWith(tString.toLowerCase())){
							int l = words.length;
							FSIterator<Annotation> tokenIteratorInner = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
							boolean stop = false, start=false;
							int count = 0;
							List<EnjuToken> tokens = new ArrayList<EnjuToken>();
							while(tokenIteratorInner.hasNext() && !stop){
								EnjuToken currTokenInner = (EnjuToken) tokenIteratorInner.next();
								if(!start && currTokenInner.getBase().equals(currToken.getBase()) && currTokenInner.getBegin()==currToken.getBegin()
										&& currTokenInner.getEnd()==currToken.getEnd()){
									start = true;
									tokens.add(currTokenInner);
									count++;
								}
								if(start && words[count].toLowerCase().equals(currTokenInner.getBase().toLowerCase())){
									tokens.add(currTokenInner);
									count++;
									if(count>=l){
										stop=true;
									}
								}
								else if(start){
									start = false;
									count = 0;
									tokens = new ArrayList<EnjuToken>();
								}
							}

							for(EnjuToken currTokenL : tokens){


								SyntacticAnnotation arg2Node = currTokenL.getArg2();
								if(!(arg2Node == null)){
									if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
										EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
										EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
										if(semHeadToken != null){
											boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
											if(!semHeadIsATrigger){
												String middleWordLemma = semHeadToken.getBase();
												SyntacticAnnotation secondArg1Node = semHeadToken.getArg1();
												if(semHeadMatches(jcas, secondArg1Node, triggerBegin, triggerEnd)){
													int countOccurences = 1;
													String featureString = cue.replaceAll(" ", "_")+"+"+tString + "_"+ middleWordLemma + "_D1_Arg2_Arg1";
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
							}
						}// if clue found
					}//while tokenIterator
				}
			}
		}
	}
	
	private void D1Arg2Arg2MW(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();



		String sentenceText = sentence.getCoveredText();
		sentenceText = lemmatised(sentenceText);
		List<String> cuesToTest = new ArrayList<String>();
		for(String cue : clueList){
			if(sentenceText.toLowerCase().contains(cue.toLowerCase())){
				cuesToTest.add(cue.toLowerCase());
			}
		}
		if(!cuesToTest.isEmpty()){
			for(String cue : cuesToTest){
				String [] words = cue.split(" ");
				if(words.length==1){
					D1Arg2Arg2(event, sentence, jcas, cue);
				}
				else{

					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
					while (tokenIterator.hasNext()){
						EnjuToken currToken = (EnjuToken) tokenIterator.next();
						String tString = currToken.getBase();

						if(cue.startsWith(tString.toLowerCase())){
							int l = words.length;
							FSIterator<Annotation> tokenIteratorInner = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
							boolean stop = false, start=false;
							int count = 0;
							List<EnjuToken> tokens = new ArrayList<EnjuToken>();
							while(tokenIteratorInner.hasNext() && !stop){
								EnjuToken currTokenInner = (EnjuToken) tokenIteratorInner.next();
								if(!start && currTokenInner.getBase().equals(currToken.getBase()) && currTokenInner.getBegin()==currToken.getBegin()
										&& currTokenInner.getEnd()==currToken.getEnd()){
									start = true;
									tokens.add(currTokenInner);
									count++;
								}
								if(start && words[count].toLowerCase().equals(currTokenInner.getBase().toLowerCase())){
									tokens.add(currTokenInner);
									count++;
									if(count>=l){
										stop=true;
									}
								}
								else if(start){
									start = false;
									count = 0;
									tokens = new ArrayList<EnjuToken>();
								}
							}

							for(EnjuToken currTokenL : tokens){


								SyntacticAnnotation arg2Node = currTokenL.getArg2();
								if(!(arg2Node == null)){
									if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
										EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
										EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
										if(semHeadToken != null){
											boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
											if(!semHeadIsATrigger){
												String middleWordLemma = semHeadToken.getBase();
												SyntacticAnnotation secondArg2Node = semHeadToken.getArg2();
												if(semHeadMatches(jcas, secondArg2Node, triggerBegin, triggerEnd)){
													int countOccurences = 1;
													String featureString = cue.replaceAll(" ", "_")+"+"+tString + "_"+ middleWordLemma + "_D1_Arg2_Arg2";
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
							}
						}// if clue found
					}//while tokenIterator
				}
			}
		}
	}
	
				private void D1Arg2Arg2(Event event, EnjuSentence sentence, JCas jcas, String cue) {
					FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
					int triggerBegin = event.getBegin();
					int triggerEnd = event.getEnd();

					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
					while (tokenIterator.hasNext()){
						EnjuToken currToken = (EnjuToken) tokenIterator.next();
						String tString = currToken.getBase();

						if(cue.equals(tString)){
							SyntacticAnnotation arg2Node = currToken.getArg2();
							if(!(arg2Node == null)){
								if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
									EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
									EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
									if(semHeadToken != null){
										boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
										if(!semHeadIsATrigger){
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
						}// if clue found
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

						if(cue.equals(tString)){
							SyntacticAnnotation arg2Node = currToken.getArg2();
							if(!(arg2Node == null)){
								if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
									EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
									EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
									if(semHeadToken != null){
										boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
										if(!semHeadIsATrigger){
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
						}// if clue found
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

						if(cue.equals(tString)){
							SyntacticAnnotation arg1Node = currToken.getArg1();
							if(!(arg1Node == null)){
								if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
									EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
									EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
									if(semHeadToken != null){
										boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
										if(!semHeadIsATrigger){
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
						}// if clue found
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

						if(cue.equals(tString)){
							SyntacticAnnotation arg1Node = currToken.getArg1();
							if(!(arg1Node == null)){
								if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
									EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
									EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
									if(semHeadToken != null){
										boolean semHeadIsATrigger = isATrigger(semHeadToken, eventIterator);
										if(!semHeadIsATrigger){
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
						}// if clue found
					}//while tokenIterator

				}

				private void D0Arg1MW(Event event, EnjuSentence sentence, JCas jcas) {

					int triggerBegin = event.getBegin();
					int triggerEnd = event.getEnd();
					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);

					String sentenceText = sentence.getCoveredText();
					sentenceText = lemmatised(sentenceText);
					List<String> cuesToTest = new ArrayList<String>();
					for(String cue : clueList){
						if(sentenceText.toLowerCase().contains(cue.toLowerCase())){
							cuesToTest.add(cue.toLowerCase());
						}
					}
					if(!cuesToTest.isEmpty()){
						for(String cue : cuesToTest){
							String [] words = cue.split(" ");
							if(words.length==1){
								D0Arg1(event, sentence, jcas, cue);
							}
							else{
								while (tokenIterator.hasNext()){
									EnjuToken currToken = (EnjuToken) tokenIterator.next();
									String tString = currToken.getBase();
									if(cue.startsWith(tString.toLowerCase())){
										int l = words.length;
										FSIterator<Annotation> tokenIteratorInner = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
										boolean stop = false, start=false;
										int count = 0;
										List<EnjuToken> tokens = new ArrayList<EnjuToken>();
										while(tokenIteratorInner.hasNext() && !stop){
											EnjuToken currTokenInner = (EnjuToken) tokenIteratorInner.next();
											if(!start && currTokenInner.getBase().equals(currToken.getBase()) && currTokenInner.getBegin()==currToken.getBegin()
													&& currTokenInner.getEnd()==currToken.getEnd()){
												start = true;
												tokens.add(currTokenInner);
												count++;
											}
											if(start && words[count].toLowerCase().equals(currTokenInner.getBase().toLowerCase())){
												tokens.add(currTokenInner);
												count++;
												if(count>=l){
													stop=true;
												}
											}
											else if(start){
												start = false;
												count = 0;
												tokens = new ArrayList<EnjuToken>();
											}
										}

										for(EnjuToken currTokenL : tokens){
											SyntacticAnnotation arg1Node = currTokenL.getArg1();
											if(!(arg1Node == null)){
												if(semHeadMatches(jcas, arg1Node, triggerBegin, triggerEnd)){
													int countOccurences = 1;
													String featureString = cue.replaceAll(" ", "_")+"+"+tString + "_D0_Arg1" ;
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
								}//while tokenIterator

							}
						}
					}



				}

				private void D0Arg2MW(Event event, EnjuSentence sentence, JCas jcas) {

					int triggerBegin = event.getBegin();
					int triggerEnd = event.getEnd();
					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);

					String sentenceText = sentence.getCoveredText();
					sentenceText = lemmatised(sentenceText);
					List<String> cuesToTest = new ArrayList<String>();
					for(String cue : clueList){
						if(sentenceText.toLowerCase().contains(cue.toLowerCase())){
							cuesToTest.add(cue.toLowerCase());
						}
					}
					if(!cuesToTest.isEmpty()){
						for(String cue : cuesToTest){
							String [] words = cue.split(" ");
							if(words.length==1){
								D0Arg2(event, sentence, jcas, cue);
							}
							else{
								while (tokenIterator.hasNext()){
									EnjuToken currToken = (EnjuToken) tokenIterator.next();
									String tString = currToken.getBase();
									if(cue.startsWith(tString.toLowerCase())){
										int l = words.length;
										FSIterator<Annotation> tokenIteratorInner = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
										boolean stop = false, start=false;
										int count = 0;
										List<EnjuToken> tokens = new ArrayList<EnjuToken>();
										while(tokenIteratorInner.hasNext() && !stop){
											EnjuToken currTokenInner = (EnjuToken) tokenIteratorInner.next();
											if(!start && currTokenInner.getBase().equals(currToken.getBase()) && currTokenInner.getBegin()==currToken.getBegin()
													&& currTokenInner.getEnd()==currToken.getEnd()){
												start = true;
												tokens.add(currTokenInner);
												count++;
											}
											if(start && words[count].toLowerCase().equals(currTokenInner.getBase().toLowerCase())){
												tokens.add(currTokenInner);
												count++;
												if(count>=l){
													stop=true;
												}
											}
											else if(start){
												start = false;
												count = 0;
												tokens = new ArrayList<EnjuToken>();
											}
										}

										for(EnjuToken currTokenL : tokens){
											SyntacticAnnotation arg2Node = currTokenL.getArg1();
											if(!(arg2Node == null)){
												if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
													int countOccurences = 1;
													String featureString = cue.replaceAll(" ", "_")+"+"+tString + "_D0_Arg1" ;
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
								}//while tokenIterator

							}
						}
					}



				}


				private void D0Arg2(Event event, EnjuSentence sentence, JCas jcas, String cue) {
					int triggerBegin = event.getBegin();
					int triggerEnd = event.getEnd();
					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
					while (tokenIterator.hasNext()){
						EnjuToken currToken = (EnjuToken) tokenIterator.next();
						String tString = currToken.getBase();
						if(cue.equals(tString)){
							
							SyntacticAnnotation arg2Node = currToken.getArg2();
							if(!(arg2Node == null)){
								if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
									int countOccurences = 1;
									String featureString = tString + "_D0_Arg2" ;
									if(!event.getSpeculation()){
										if(clueMapD0.containsKey(featureString))
											countOccurences = countOccurences + clueMapD0.get(featureString);
										clueMapD0.put(featureString, countOccurences);
										if(!unclueMapD0.containsKey(featureString))
											unclueMapD0.put(featureString, 0);
									}
									else{
										if(unclueMapD0.containsKey(featureString))
											countOccurences = countOccurences + unclueMapD0.get(featureString);
										unclueMapD0.put(featureString, countOccurences);
										if(!clueMapD0.containsKey(featureString))
											clueMapD0.put(featureString, 0);
									}
									if(!ruleKeyListD0.contains(featureString)){
										ruleKeyListD0.add(featureString);
									}
									rcount++;
								}
							}
						}
					}//while tokenIterator

				}

				private String lemmatised(String sentenceText) {
					Lemmatiser lemmatiser = new Lemmatiser();
					List<String> lemmas = lemmatiser.lemmatize(sentenceText);
					String phrase = "";
					for(String lemma : lemmas){
						phrase+=lemma+" ";
					}
					phrase = phrase.substring(0,phrase.lastIndexOf(" "));
					return phrase;
				}

				private void D0Arg1(Event event, EnjuSentence sentence, JCas jcas, String cue) {
					int triggerBegin = event.getBegin();
					int triggerEnd = event.getEnd();
					FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
					while (tokenIterator.hasNext()){
						EnjuToken currToken = (EnjuToken) tokenIterator.next();
						String tString = currToken.getBase();
						if(cue.equals(tString)){
							
							SyntacticAnnotation arg2Node = currToken.getArg1();
							if(!(arg2Node == null)){
								if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
									int countOccurences = 1;
									String featureString = tString + "_D0_Arg1" ;
									if(!event.getSpeculation()){
										if(clueMapD0.containsKey(featureString))
											countOccurences = countOccurences + clueMapD0.get(featureString);
										clueMapD0.put(featureString, countOccurences);
										if(!unclueMapD0.containsKey(featureString))
											unclueMapD0.put(featureString, 0);
									}
									else{
										if(unclueMapD0.containsKey(featureString))
											countOccurences = countOccurences + unclueMapD0.get(featureString);
										unclueMapD0.put(featureString, countOccurences);
										if(!clueMapD0.containsKey(featureString))
											clueMapD0.put(featureString, 0);
									}
									if(!ruleKeyListD0.contains(featureString)){
										ruleKeyListD0.add(featureString);
									}
									rcount++;
								}
							}
						}
					}//while tokenIterator

				}

				private ArrayList<String> getListfromMap(ArrayList<String> hits,
						HashMap<String,Integer> map) {
					//System.out.println("START");
					for(String s : map.keySet()){
						//System.out.println(s);
						hits.add(Integer.toString(map.get(s)));
					}
					//System.out.println("END");
					return hits;
				}


				private ArrayList<String> intitialiseClueList(File file) {
					clueList = new ArrayList<String>();
					try {
						BufferedReader reader = new BufferedReader(new FileReader(file) );
						String line;
						while((line = reader.readLine())!=null){
							if(!clueList.contains(line))
								clueList.add(line);
							//    System.out.println(line);
						}
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return clueList;
				}


				private ArrayList<String> intitialiseClueListMW(File file) {
					clueList = new ArrayList<String>();
					clueListInit = new HashMap<String,List<String>>();
					try {
						BufferedReader reader = new BufferedReader(new FileReader(file) );
						String line;
						while((line = reader.readLine())!=null){
							if(!clueList.contains(line))
								clueList.add(line);
							//    System.out.println(line);
							String [] words = line.split(" ");
							if(words.length>1){
								String firstWord = words[0];
								List<String> phrases = clueListInit.get(firstWord);
								if(phrases==null){
									phrases = new ArrayList<String>();

								}
								phrases.add(line);
								clueListInit.put(firstWord, phrases);
							}
						}
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return clueList;
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

