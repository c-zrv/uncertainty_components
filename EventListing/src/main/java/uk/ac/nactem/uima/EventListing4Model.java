
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


public class EventListing4Model extends JCasAnnotator_ImplBase {


	public static final String PARAM_NAME_OUTFILE = "OutFile";


	Map<String, List<String>> entitySynonyms;

	public File outFile ;
	Map<String,String> triggerMap;
	Double alpha;
	Boolean strict;
	private boolean modelfound = false;
	int newEventCounter, newEntityCounter;
	Map<String, Integer> newEntityIdMap = new HashMap<String, Integer>();
	List<String> entityPairs;

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

			if(!(jcas.getDocumentText().length()>1) && !modelfound){
				modelfound=true;
				entitySynonyms = getEntitySynonyms(jcas);
				processFirstModel(jcas);
			}


		}catch(Exception e){
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}

	}




	

	public List<String> processEventArguments(Event event){

		List<String> argumentList = new ArrayList<String>();

		FSArray arguments = event.getArguments();
		if(event.getNormalisedName()!=null){

			if(arguments!=null){
				for(int i =0; i < arguments.size(); i++){
					EventArgument argument = (EventArgument) arguments.get(i);
					if(argument != null){
						if(argument.getRole().toLowerCase().contains("arg2") || argument.getRole().toLowerCase().contains("arg1") && argument.getValue()!=null){
							Concept target = (Concept) argument.getValue();
							if(argument.getValue()!=null){
								String arg = target.getNormalisedName();
								if(!argumentList.contains(arg.toLowerCase()) && arg!=null)
									argumentList.add(arg.toLowerCase());
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

	private void processFirstModel(JCas jcas) throws AnalysisEngineProcessException {
		//try {

			System.out.println("DBG: Start processing first model");
			FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).iterator();
			while (eventIterator.hasNext()) {

				Event event = (Event) eventIterator.next();
				String eventId = event.getId();

				String eventType = event.getNormalisedName();
				/*		if(triggerMap.containsKey(eventType)){
					eventType = triggerMap.get(eventType);
				}*/
				
				List<String> args = processEventArguments(event);
			}
				/*
				EventModel eventFormat = new EventModel(event, eventType, entitySynonyms, event.getModelElement());
				List<EventModel> events = eventModelMap.get(eventFormat.getStringFormat());
				if(events == null){
					events = new ArrayList<EventModel>();
				}
				events.add(eventFormat);
				writer.write("Added: "+eventId + "to the list of events with size "+ events.size());
				writer.newLine();
				eventModelMap.put(eventFormat.getStringFormat(), events);
				writer.write("Event string: "+ eventFormat.getStringFormat());
				writer.newLine();

				EventExtended extendedEvent = new EventExtended(eventFormat);

				List<EventExtended> extendedMaps = eventMapExtended.get(eventFormat.getStringFormat());
				if(extendedMaps == null){
					extendedMaps = new ArrayList<EventExtended>();
				}
				extendedMaps.add(extendedEvent);
				eventMapExtended.put(eventFormat.getStringFormat(), extendedMaps);
				String parg = eventFormat.getPrimaryArgString();
				if(parg!=null){

					if(getSynonym(parg)!=null){
						parg= getSynonym(parg);
					}
				}
				writer.write("Parg string: "+ parg);
				writer.newLine();
				System.out.println("DBG: Parg string: "+ parg);
				String sarg = eventFormat.getSecondaryArgString();
				if(sarg!=null){
					if(getSynonym(sarg) != null){
						sarg = getSynonym(sarg);
					}
				}
				writer.write("Sarg string: "+ sarg);
				writer.newLine();
				System.out.println("DBG: Sarg string: "+ sarg);
				writer.write("Adding model event: "+ eventId);
				writer.newLine();
				//save for later on retrieval

				add2EntityList(parg,eventFormat);
				add2EntityList(sarg,eventFormat);

				writer.write("Finished with event: "+ eventId);
				writer.newLine();
				System.out.println("DBG: Finished with event: "+ eventId);
			}
			writer.write("Finish processing model");
			writer.newLine();
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new AnalysisEngineProcessException(e);
		}
*/
	}


/*
	private void add2EntityList(String arg, EventModel eventFormat) {
		List<EventModel> p = eventEntityMap.get(arg);
		if(p == null){
			p = new ArrayList<EventModel>();
		}
		p.add(eventFormat);
		eventEntityMap.put(arg, p);

	}
*/
	



	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		try {
			BufferedWriter writerlog = new BufferedWriter(new FileWriter("",true)); //TODO have different logfile for final model
			writerlog.write("Start writing json");
			writerlog.newLine();
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			JsonArray json = new JsonArray();
			
			writerlog.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	
	public Map<String, List<String>> getEntitySynonyms(JCas jcas){
		Map<String, List<String>> synonyms = new HashMap<String, List<String>>();
		Map<String, Set<String>> synonymSets = new HashMap<String, Set<String>>();

		FSIterator<Annotation> ggpIterator = jcas.getAnnotationIndex(GeneOrProtein.type).iterator();
		while (ggpIterator.hasNext()) {
			GeneOrProtein ggp = (GeneOrProtein) ggpIterator.next();
			StringArray syns = ggp.getSynonyms();
			Set<String> synset = synonymSets.get(ggp.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(ggp.getNormalisedName().toLowerCase(), ggp.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(ggp.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, ggp.getModelElement());
			}
			synonymSets.put(ggp.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> pfIterator = jcas.getAnnotationIndex(ProteinFamily.type).iterator();
		while (pfIterator.hasNext()) {
			ProteinFamily pf = (ProteinFamily) pfIterator.next();
			StringArray syns = pf.getSynonyms();
			Set<String> synset = synonymSets.get(pf.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(pf.getNormalisedName().toLowerCase(), pf.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(pf.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, pf.getModelElement());
			}
			synonymSets.put(pf.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> pcIterator = jcas.getAnnotationIndex(ProteinComplex.type).iterator();
		while (pcIterator.hasNext()) {
			ProteinComplex pc = (ProteinComplex) pcIterator.next();
			StringArray syns = pc.getSynonyms();
			Set<String> synset = synonymSets.get(pc.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(pc.getNormalisedName().toLowerCase(), pc.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(pc.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, pc.getModelElement());
			}
			synonymSets.put(pc.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> psIterator = jcas.getAnnotationIndex(ProteinSite.type).iterator();
		while (psIterator.hasNext()) {
			ProteinSite ps = (ProteinSite) psIterator.next();
			StringArray syns = ps.getSynonyms();
			Set<String> synset = synonymSets.get(ps.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(ps.getNormalisedName().toLowerCase(), ps.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(ps.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, ps.getModelElement());
			}
			synonymSets.put(ps.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> cdIterator = jcas.getAnnotationIndex(ChemicalOrDrug.type).iterator();
		while (cdIterator.hasNext()) {
			ChemicalOrDrug cd = (ChemicalOrDrug) cdIterator.next();
			StringArray syns = cd.getSynonyms();
			Set<String> synset = synonymSets.get(cd.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(cd.getNormalisedName().toLowerCase(), cd.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(cd.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, cd.getModelElement());
			}
			synonymSets.put(cd.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> dcIterator = jcas.getAnnotationIndex(DrugClass.type).iterator();
		while (dcIterator.hasNext()) {
			DrugClass dc = (DrugClass) dcIterator.next();
			StringArray syns = dc.getSynonyms();
			Set<String> synset = synonymSets.get(dc.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(dc.getNormalisedName().toLowerCase(), dc.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(dc.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, dc.getModelElement());
			}
			synonymSets.put(dc.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> pathIterator = jcas.getAnnotationIndex(Pathway.type).iterator();
		while (pathIterator.hasNext()) {
			Pathway path = (Pathway) pathIterator.next();
			StringArray syns = path.getSynonyms();
			Set<String> synset = synonymSets.get(path.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(path.getNormalisedName().toLowerCase(), path.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(path.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, path.getModelElement());
			}
			synonymSets.put(path.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> sclIterator = jcas.getAnnotationIndex(SubcellularLocation.type).iterator();
		while (sclIterator.hasNext()) {
			SubcellularLocation scl = (SubcellularLocation) sclIterator.next();
			StringArray syns = scl.getSynonyms();
			Set<String> synset = synonymSets.get(scl.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(scl.getNormalisedName().toLowerCase(), scl.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(scl.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, scl.getModelElement());
			}
			synonymSets.put(scl.getNormalisedName().toLowerCase(), synset);
		}

		FSIterator<Annotation> clIterator = jcas.getAnnotationIndex(CellLine.type).iterator();
		while (clIterator.hasNext()) {
			CellLine cl = (CellLine) clIterator.next();
			StringArray syns = cl.getSynonyms();
			Set<String> synset = synonymSets.get(cl.getNormalisedName().toLowerCase());
			if(synset==null){
				synset = new HashSet<String>();
			}
			//entityModelIdMap.put(cl.getNormalisedName().toLowerCase(), cl.getModelElement());
			for(int i =0; i<syns.size(); i++){
				String syn = syns.get(i).toLowerCase();
				List<String> rootNames = synonyms.get(i);
				if(rootNames==null){
					rootNames = new ArrayList<String>();
				}
				rootNames.add(cl.getNormalisedName().toLowerCase());
				synonyms.put(syn, rootNames);
				synset.add(syn);
				//entityModelIdMap.put(syn, cl.getModelElement());
			}
			synonymSets.put(cl.getNormalisedName().toLowerCase(), synset);
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
		return synonyms;
	}


	private String getSynonym(String arg) {
		if(arg!=null){
			List<String> synonyms = entitySynonyms.get(arg.toLowerCase());
			if(synonyms!=null)
				return synonyms.get(0);
			else
				return null;
		}
		return null;
	}

	
}

