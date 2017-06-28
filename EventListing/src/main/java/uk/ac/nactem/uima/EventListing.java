
package uk.ac.nactem.uima;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import uk.ac.nactem.uima.bigm.CellLine;
import uk.ac.nactem.uima.bigm.ChemicalOrDrug;
import uk.ac.nactem.uima.bigm.Concept;
import uk.ac.nactem.uima.bigm.DrugClass;
import uk.ac.nactem.uima.bigm.EventArgument;
import uk.ac.nactem.uima.bigm.EventAttribute;
import uk.ac.nactem.uima.bigm.GeneOrProtein;
import uk.ac.nactem.uima.bigm.Pathway;
import uk.ac.nactem.uima.bigm.ProteinComplex;
import uk.ac.nactem.uima.bigm.ProteinFamily;
import uk.ac.nactem.uima.bigm.ProteinSite;
import uk.ac.nactem.uima.bigm.Score;
import uk.ac.nactem.uima.bigm.Sentence;
import uk.ac.nactem.uima.bigm.SubcellularLocation;
import uk.ac.nactem.uima.bigm.Event;

public class EventListing extends JCasAnnotator_ImplBase {


	public static final String PARAM_NAME_OUTFILE = "OutFile";

	List<String> entityPairs;
	public File  outFile ;
	
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		entityPairs = new ArrayList<String>();
		outFile = new File((String)(aContext.getConfigParameterValue(PARAM_NAME_OUTFILE)));
	
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		try {
			Iterator<CAS> viewIterator = jcas.getCas().getViewIterator();
			int viewCtr = 1;
			while (viewIterator.hasNext()) {
				System.out.println("IN VIEW " + viewCtr++);
				viewIterator.next();
			}

			if (jcas.getDocumentText().length()>1 ){
				FSIterator<Annotation> doc = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
				SourceDocumentInformation paper = (SourceDocumentInformation)doc.next();
				System.out.println("Processing: "+paper.getUri());
				processDocument(jcas);
			}

		}catch(Exception e){
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}

	}

	public List<String> processEventArguments(Event event, Sentence sentence){

		List<String> argumentList = new ArrayList<String>();
		FSArray arguments = event.getArguments();
		if(event.getNormalisedName()!=null){
			String eventType = event.getNormalisedName();
			eventType = eventType.toLowerCase();
			if(event.getCoveredText()!=null){
				if(arguments!=null){
					for(int i =0; i < arguments.size(); i++){
						EventArgument argument = (EventArgument) arguments.get(i);
						if(argument!=null){
							if(argument.getRole().toLowerCase().contains("arg2") || argument.getRole().toLowerCase().contains("arg1") && argument.getValue()!=null){
								Concept target = (Concept) argument.getValue();
								if(argument.getValue()!=null){
								String arg = target.getCoveredText();
								if(!argumentList.contains(arg.toLowerCase()) && arg!=null)
									argumentList.add(arg.toLowerCase());
								}
							}
						}
					}
				}
			}
		}
		if(argumentList.size()==2){
			return argumentList;
		}
		return null;
	}



	private void processDocument(JCas jcas) throws AnalysisEngineProcessException {


		FSIterator<Annotation> doc = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
		SourceDocumentInformation paper = (SourceDocumentInformation)doc.next();
		String paperName = paper.getUri();
		System.out.println("DBG : Parsing: "+ paperName);
		//		checkModel("document + "+ paperName);
		try {
		//	BufferedWriter writer = new BufferedWriter(new FileWriter(logFileP,true));
		//	writer.write("Parsing: "+ paperName);
		//	writer.newLine();
			FSIterator<Annotation> sentenceIterator = jcas.getAnnotationIndex(Sentence.type).iterator();
			while (sentenceIterator.hasNext()) {
				Sentence sentence = (Sentence) sentenceIterator.next();
		//		writer.write("found next sentence: \t" +sentence.getCoveredText());
		//		writer.newLine();
				FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
				while (eventIterator.hasNext()) {
					Event event = (Event) eventIterator.next();
			//		writer.write("found next event: " +event.getId());
			//		writer.newLine();
					List<String> args = processEventArguments(event, sentence);
					if(args!=null){
						add2List(args);
					}
				}
				
			}

		//	writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}
	}



	private void add2List(List<String> args) {
		String str1 = args.get(0);
		String str2 = args.get(1);
		String conc1 = str1+"\t"+str2;
		String conc2 = str2+"\t"+str1;
		if(!entityPairs.contains(conc1)){
			entityPairs.add(conc1);
		}
		if(!entityPairs.contains(conc2)){
			entityPairs.add(conc2);
		}
	}


	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile,true)); //TODO have different logfile for final model
			java.util.Collections.sort(entityPairs);
			for (String pair : entityPairs){
				writer.write(pair);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	



	
		//------------NEW TYPES (unused)-----------//
		/*
		FSIterator<Annotation> geneIterator = jcas.getAnnotationIndex(Gene.type).iterator();
		while (geneIterator.hasNext()) {
			Gene gene = (Gene) geneIterator.next();
			StringArray syns = gene.getSynonyms();
			Set<String> synset = synonymSets.get(gene.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}

			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(gene.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
			}
			synonymSets.put(gene.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> pIterator = jcas.getAnnotationIndex(Protein.type).iterator();
		while (pIterator.hasNext()) {
			Protein p = (Protein) pIterator.next();
			StringArray syns = p.getSynonyms();
			Set<String> synset = synonymSets.get(p.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}

			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(p.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
			}
			synonymSets.put(p.getNormalisedName().toLowerCase(), synset);
		}
		 */
	

}

