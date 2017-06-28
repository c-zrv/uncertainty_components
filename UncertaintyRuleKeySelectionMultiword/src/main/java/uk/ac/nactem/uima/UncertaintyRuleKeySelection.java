package uk.ac.nactem.uima;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent;
import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuSentence;
import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuToken;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.u_compare.shared.syntactic.SyntacticAnnotation;

import uk.ac.nactem.uima.cas.bionlpst.Event;
import uk.ac.nactem.uima.ml.Instance;
import uk.ac.nactem.uima.ml.NameValuePair;



public class UncertaintyRuleKeySelection extends JCasAnnotator_ImplBase {

	private ArrayList<String> clueList;
	private ArrayList<String> featNameList;
	private LinkedHashMap<String, Integer> clueMap;
	private ArrayList<String []> featureList;
	private ArrayList<Event> eventList;

	private static final String clueFile = "clues";

	//	private ArrayList<Integer[]> xValues;
	//	private ArrayList<Integer> yValues;
	private ArrayList<String> fNames;
	private int k;
	private File ff, clues_rules_out;
	private int [][] features;
	int y1count, y0count;


	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		try {
			clueList = intitialiseClueList(new File(aContext.getResourceFilePath(clueFile)));
			intitialiseMap();
			featureList = new ArrayList<String []>();
			initialiseNames();
			//	xValues = new ArrayList<Integer[]>();
			//	yValues = new ArrayList<Integer>();
			fNames = new ArrayList<String>();
			features = null;
			k = 300;
			clues_rules_out = new File("resources/clues_rules_PMI_new.txt");
			ff = new File("resources/out.txt");
			y1count = 0;
			y0count = 0;
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	private void initialiseNames() {
		featNameList = new ArrayList<String>();
		String [] rules = {"D0Arg1_","D0Arg2_","D1Arg1Arg1_","D1Arg1Arg2_","D1Arg2Arg1_","D1Arg2Arg2_","D1beArg1Arg1_","D1beArg1Arg2_","D1beArg2Arg1_","D1beArg2Arg2_",
				"D1ofArg1Arg1_","D1ofArg1Arg2_","D1ofArg2Arg1_","D1ofArg2Arg2_", "aolkArg1not_", "aolkArg2not_"};
		for(String s : rules){
			for(String cs : clueList){
				featNameList.add(s+cs);
			}
		}
		eventList = new ArrayList<Event>();
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> sentenceIterator = jcas.getAnnotationIndex(EnjuSentence.type).iterator();
		while (sentenceIterator.hasNext()) {
			EnjuSentence currSentence = (EnjuSentence) sentenceIterator.next();

			FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(currSentence);
			while (eventIterator.hasNext()) {
				Event event = (Event) eventIterator.next();
				ArrayList<String> allFeatures = new ArrayList<String>();

				allFeatures.addAll(D0Arg1(event, currSentence, jcas));
				allFeatures.addAll(D0Arg2(event, currSentence, jcas));
				allFeatures.addAll(D1Arg1Arg1(event, currSentence, jcas));
				allFeatures.addAll(D1Arg1Arg2(event, currSentence, jcas));
				allFeatures.addAll(D1Arg2Arg1(event, currSentence, jcas));
				allFeatures.addAll(D1Arg2Arg2(event, currSentence, jcas));
				allFeatures.addAll(D1beArg1Arg1(event, currSentence, jcas));
				allFeatures.addAll(D1beArg1Arg2(event, currSentence, jcas));
				allFeatures.addAll(D1beArg2Arg1(event, currSentence, jcas));
				allFeatures.addAll(D1beArg2Arg2(event, currSentence, jcas));
				allFeatures.addAll(D1ofArg1Arg1(event, currSentence, jcas));
				allFeatures.addAll(D1ofArg1Arg2(event, currSentence, jcas));
				allFeatures.addAll(D1ofArg2Arg1(event, currSentence, jcas));
				allFeatures.addAll(D1ofArg2Arg2(event, currSentence, jcas));
				//				allFeatures.addAll(aolkArg1(event, currSentence, jcas));
				//				allFeatures.addAll(aolkArg2(event, currSentence, jcas));
				allFeatures.addAll(aolkArg1not(event, currSentence, jcas));
				allFeatures.addAll(aolkArg2not(event, currSentence, jcas));
				//	System.out.println("DONE");
				if(allFeatures.size()!= featNameList.size()){
					System.out.println("Error: Feature vector size and feature names size do do match : " + featNameList.size() + " : " +  allFeatures.size());
				}
				String [] stringfeatures = (String[]) allFeatures.toArray(new String[allFeatures.size()]);
				//	featureList.add(stringfeatures);

				//	eventList.add(event);
				if(features==null){
					features= new int [stringfeatures.length][2];
				}
				for(int i =0; i<stringfeatures.length; i++){
					if (event.getSpeculation()){
						features[i][1] = features[i][1]+Integer.parseInt(stringfeatures[i]);
						y1count ++;
					}
					else{
						features[i][0] = features[i][0]+Integer.parseInt(stringfeatures[i]);
						y0count ++;
					}
				}
			}//while eventIterator
		}//while sentenceIterator
	
	}

	
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		try{
		
			Integer [] pC1 = new Integer [features.length]; 
			Integer [] pC0 = new Integer [features.length];
			pC1 = initialise20(pC1);
			pC0 = initialise20(pC0);
			double[] pmiC1  = new double [features.length];
			System.out.println("feature length: "+ features.length);
			for(int i = 0; i<pC1.length; i++){
				//	System.out.println((double)pC0[i]);
				//	System.out.println((double)((double)l0Count/(double)(l0Count+l1Count)));
				double denominator1 = ((double)(features[i][0]+features[i][1])/(double)(y0count+y1count)) ;
				double numerator1 = ((double)features[i][1]/(double)(y1count));
				//	System.out.println(numerator1 + " : " + denominator1);
				//	double val0 =  Math.log((double)pC0[i]/(double)((double)l0Count/(double)(l0Count+l1Count)));
				double val1 = Math.log(numerator1/denominator1);
				//	if(Double.isNaN(val0)){
				//		val0 = 0;
				//	}
				if(Double.isNaN(val1)){
					//		System.out.println("NAN : "+ val1);
					val1 = 0;
				}
				//	System.out.println(val1);
				//	pmiC0[i] = val0;
				pmiC1[i] = val1;
			}

			//		Double [] topKpmiC1 = Arrays.copyOf(pmiC1, pmiC1.length); 
			//		Double [] topKpmiC0 = Arrays.copyOf(pmiC0, pmiC0.length); 

			ArrayList<Double> top1 = new ArrayList<Double>();
			ArrayList<Integer> top1idx = new ArrayList<Integer>();
			ArrayList<String> top1name = new ArrayList<String>();

			while(top1.size()< k){
				Double largest = 0.0;
				int index = 0;
				for (int i = 0; i < pmiC1.length; i++) {
					//	System.out.println(pmiC1[i]);
					if ( pmiC1[i] - largest >0 ) {
						largest = pmiC1[i];
						index = i;

					}
				}
				pmiC1[index] = 0.0;
				top1.add(new Double(largest));
				top1idx.add(new Integer(index));
			}

			for(int i : top1idx){
				top1name.add(featNameList.get(i));
			}

			BufferedWriter writer = new BufferedWriter (new FileWriter(ff));
			for (int i = 0; i<k; i++){
				writer.write(top1name.get(i) + "\t" + top1idx.get(i) + "\t" +top1.get(i));
				writer.newLine();
			}
			writer.close();
			
			
			BufferedWriter writer2 = new BufferedWriter (new FileWriter(clues_rules_out));
			for (int i = 0; i<k; i++){
				if(top1.get(i)>0.1){
					writer2.write(top1name.get(i));
					writer2.newLine();
				}
			}
			writer2.close();
		}

		catch (Exception e) {
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}
		super.collectionProcessComplete();
	}


	private Double[] initialise20(Double[] array) {
		for(int i =0; i<array.length; i++){
			array[i]=0.0;
		}
		return array;
	}

	private Integer[] initialise20(Integer[] array) {
		for(int i =0; i<array.length; i++){
			array[i]=0;
		}
		return array;

	}

	private Collection<? extends String> aolkArg1(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
						map.put(tString, 1);
					}
				}
			}//if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> aolkArg2(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			if(clueList.contains(tString)){
				SyntacticAnnotation arg2Node = currToken.getArg2();	
				if(!(arg2Node == null)){
					if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
						map.put(tString, 1);
					}
				}
			}//if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}

	private Collection<? extends String> aolkArg2not(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
								map.put(tString, 1);
								found = true;
							}
						}
						if (!found){
							arg2Node = previous.getArg2();	
							if(!(arg2Node == null)){
								if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
									map.put(tString, 1);
									found = true;
								}
							}
						}
					}
				}// if not found
			}//if clue found
			previous = currToken;
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> aolkArg1not(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
								map.put(tString, 1);
								found = true;
							}
						}
						if (!found){
							arg1Node = previous.getArg1();	
							if(!(arg1Node == null)){
								if(semHeadMatches(jcas, arg1Node, triggerBegin, triggerEnd)){
									map.put(tString, 1);
									found = true;
								}

							}
						}
					}
				}// if not found
			}//if clue found
			previous = currToken;
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1ofArg2Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			if(tString.equalsIgnoreCase("of")){
				SyntacticAnnotation arg2Node = currToken.getArg2();	
				if(!(arg2Node == null)){
					if(arg2Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
						EnjuConstituent arg2NodeEC = (EnjuConstituent) arg2Node;
						EnjuToken semHeadToken = getSemHeadToken(arg2NodeEC, jcas);
						if(semHeadToken != null){
							String clueCandidate = semHeadToken.getBase().toLowerCase();
							if (clueList.contains(clueCandidate)){
								SyntacticAnnotation secarg2Node = currToken.getArg2();	
								if(!(secarg2Node == null)){
									if(semHeadMatches(jcas, secarg2Node, triggerBegin, triggerEnd)){
										map.put(clueCandidate, 1);
									}
								}
							}
						}
					}		
				}
			}// if of found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1ofArg2Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			if(tString.equalsIgnoreCase("of")){
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
										map.put(clueCandidate, 1);
									}
								}
							}
						}
					}		
				}
			}// if "of" found
		}//while tokenIterator
		hits = getListfromMap(hits,map);

		return hits;
	}


	private Collection<? extends String> D1ofArg1Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			if(tString.equalsIgnoreCase("of")){
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
										map.put(clueCandidate, 1);
									}
								}
							}
						}
					}		
				}
			}// if of found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1ofArg1Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();

		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			if(tString.equalsIgnoreCase("of")){
				SyntacticAnnotation arg1Node = currToken.getArg1();	
				if(!(arg1Node == null)){
					if(arg1Node.getClass().getName().equalsIgnoreCase("jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent")){
						EnjuConstituent arg1NodeEC = (EnjuConstituent) arg1Node;
						EnjuToken semHeadToken = getSemHeadToken(arg1NodeEC, jcas);
						if(semHeadToken != null){
							String clueCandidate = semHeadToken.getBase().toLowerCase();
							if (clueList.contains(clueCandidate)){
								SyntacticAnnotation secarg1Node = currToken.getArg1();	
								if(!(secarg1Node == null)){
									if(semHeadMatches(jcas, secarg1Node, triggerBegin, triggerEnd)){
										map.put(clueCandidate, 1);
									}
								}
							}
						}
					}		
				}
			}// if of found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1beArg2Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
							if (semHeadToken.getBase().equalsIgnoreCase("be")){
								arg2Node = semHeadToken.getArg2();	
								if(!(arg2Node == null)){
									if(semHeadMatches(jcas, semHeadToken.getArg2(), triggerBegin, triggerEnd)){
										map.put(tString, 1);
									}		
								}
							}
						}
					}		
				}
			}// if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1beArg2Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
							if (semHeadToken.getBase().equalsIgnoreCase("be")){
								SyntacticAnnotation arg1Node = semHeadToken.getArg1();	
								if(!(arg1Node == null)){
									if(semHeadMatches(jcas, semHeadToken.getArg1(), triggerBegin, triggerEnd)){
										map.put(tString, 1);
									}		
								}
							}
						}
					}		
				}
			}// if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1beArg1Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
							if (semHeadToken.getBase().equalsIgnoreCase("be")){
								SyntacticAnnotation arg2Node = semHeadToken.getArg2();	
								if(!(arg2Node == null)){
									if(semHeadMatches(jcas, semHeadToken.getArg2(), triggerBegin, triggerEnd)){
										map.put(tString, 1);
									}		
								}
							}
						}
					}		
				}
			}// if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1beArg1Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
							if (semHeadToken.getBase().equalsIgnoreCase("be")){
								arg1Node = semHeadToken.getArg1();	
								if(!(arg1Node == null)){
									if(semHeadMatches(jcas, semHeadToken.getArg1(), triggerBegin, triggerEnd)){
										map.put(tString, 1);
									}		
								}
							}
						}
					}		
				}
			}// if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1Arg2Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
								SyntacticAnnotation secondArg2Node = semHeadToken.getArg2();
								if(semHeadMatches(jcas, secondArg2Node, triggerBegin, triggerEnd)){
									map.put(tString, 1);
								}
							}//if semHead is not a trigger
						}	
					}
				}
			}// if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1Arg1Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
								SyntacticAnnotation secondArg2Node = semHeadToken.getArg2();
								if(semHeadMatches(jcas, secondArg2Node, triggerBegin, triggerEnd)){
									map.put(tString, 1);
								}
							}//if semHead is not a trigger
						}	
					}
				}
			}// if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1Arg2Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
								SyntacticAnnotation secondArg1Node = semHeadToken.getArg1();
								if(semHeadMatches(jcas, secondArg1Node, triggerBegin, triggerEnd)){
									map.put(tString, 1);
								}
							}//if semHead is not a trigger
						}		
					}
				}
			}// if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D1Arg1Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
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
								SyntacticAnnotation secondArg1Node = semHeadToken.getArg1();
								if(semHeadMatches(jcas, secondArg1Node, triggerBegin, triggerEnd)){
									map.put(tString, 1);
								}
							}//if semHead is not a trigger
						}
					}		
				}
			}// if clue found
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D0Arg2(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		ArrayList<String> hits = new ArrayList<String>();
		int triggerBegin = event.getBegin();
		int triggerEnd = event.getEnd();
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		while (tokenIterator.hasNext()){
			EnjuToken currToken = (EnjuToken) tokenIterator.next();
			String tString = currToken.getBase();
			if(clueList.contains(tString)){
				SyntacticAnnotation arg2Node = currToken.getArg2();	
				if(!(arg2Node == null)){
					if(semHeadMatches(jcas, arg2Node, triggerBegin, triggerEnd)){
						map.put(tString, 1);
					}
				}
			}
		}//while tokenIterator
		hits = getListfromMap(hits,map);
		return hits;
	}


	private Collection<? extends String> D0Arg1(Event event, EnjuSentence sentence, JCas jcas) {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>(clueMap);
		/*System.out.println("Checking map");
		for(String s : map.keySet()){
			System.out.println(s);
		}
			System.out.println("end map");*/
		ArrayList<String> hits = new ArrayList<String>();
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
						map.put(tString, 1);
					}
				}
			}
		}//while tokenIterator
		hits = getListfromMap(hits, map);
		return hits;
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
				//	System.out.println(line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return clueList;
	}


	private void intitialiseMap() {
		clueMap = new LinkedHashMap<String, Integer>();
		for(String s : clueList){
	//		System.out.println(s+"__");
			clueMap.put(s, 0);
		}
		//		for(String s: clueMap.keySet()){
		//			System.out.println(s);
		//		}
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
