package uk.ac.nactem.uima;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuSentence;
import uk.ac.nactem.uima.cas.bionlpst.Attribute;
import uk.ac.nactem.uima.cas.bionlpst.Event;

public class UncertaintyListing extends JCasAnnotator_ImplBase {
	public static final String PARAM_NAME_OUTFILE = "OutputFile";
	public ArrayList<String> eventList = new ArrayList<String>();

	//map for keeping track of event attributes
	Map<String,Attribute> eventAttributeMap = new HashMap<String,Attribute>();

	public File outFile ;

	
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		try {

			super.initialize(aContext);
			outFile = new File((String) aContext.getConfigParameterValue(PARAM_NAME_OUTFILE));
		
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}


	public void process(JCas jcas) throws AnalysisEngineProcessException {
		eventAttributeMap = new HashMap<String,Attribute>();
		//RB: populate the event attribute map
		
		Type attributeType = jcas.getTypeSystem().getType("uk.ac.nactem.uima.cas.bionlpst.Attribute");
		FSIterator<FeatureStructure> attributeIterator = jcas.getIndexRepository().getAllIndexedFS(attributeType);
		while (attributeIterator.hasNext()) {
			Attribute attribute = (Attribute) attributeIterator.next();
			Event event = (Event) attribute.getAnnotation();
			if (event!=null) {
				String eventID = event.getId();
				if (attribute.getAttributeName().equals("Uncertainty")) {
					eventAttributeMap.put(eventID + ":Uncertainty", attribute);
				}
				else if (attribute.getAttributeName().equals("Polarity")) {
					eventAttributeMap.put(eventID + ":Polarity", attribute);
				}
				else if (attribute.getAttributeName().equals("CL")) {
					eventAttributeMap.put(eventID + ":CL", attribute);
				}
				else if (attribute.getAttributeName().equals("KT")) {
					eventAttributeMap.put(eventID + ":KT", attribute);
				}
			}
		}

		FSIterator<Annotation> doc = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
		SourceDocumentInformation s = (SourceDocumentInformation)doc.next();
		System.out.println(s.getUri());

		String docName = s.getUri();
		docName = docName.replaceAll("/tmp/", "");
		docName = docName.replaceAll(".txt", "");
		System.out.println(docName);
		
		FSIterator<Annotation> sentenceIterator = jcas.getAnnotationIndex(EnjuSentence.type).iterator();

		while (sentenceIterator.hasNext()) {
		
			EnjuSentence sentence = (EnjuSentence) sentenceIterator.next();
	
			//Iterate over all events of the sentence and calculate the additional features
			FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);

			while (eventIterator.hasNext()) {

				//Get event and event trigger EnjuTokens
				Event event = (Event) eventIterator.next();
				String eid = event.getId();
				System.out.println(event.getName()+"\tprior");
				String name = event.getName();
				String text = event.getCoveredText();
				String combinedName = docName + "_" + eid;
	
				Boolean uncertaintyOriginal = event.getSpeculation();
				String uncertaintyOriginalStr = "false";
				if(uncertaintyOriginal){
					uncertaintyOriginalStr = "true";
				}
				String uncertainty = eventAttributeMap.get(eid+":Uncertainty").getAttributeValue();
				String eventString;
			
				eventString = eid + "\t" + name + "\t" + text + "\t" + combinedName +  "\t" + s.getUri() + "\t" + sentence.getCoveredText() + "\t" + uncertaintyOriginalStr + "\t" +  uncertainty;
			
				eventList.add(eventString);
			}
		}
	}
	
	public void collectionProcessComplete() throws AnalysisEngineProcessException {

		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, true), "UTF-8"));
			
				writer.write("Event ID \t Event type \t Covered text \t combinedName \t Paper ID \t sentence \t CL \t Uncertainty");
			
			writer.newLine();

			for(String event : eventList){
				writer.write(event.replace("\n", " "));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Event writing finished.....");

		super.collectionProcessComplete();
	}
}