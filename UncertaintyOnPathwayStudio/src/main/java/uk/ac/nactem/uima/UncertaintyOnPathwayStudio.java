package uk.ac.nactem.uima;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.u_compare.shared.label.penn.bracket.release.S;

import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuConstituent;
import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuSentence;
import jp.ac.u_tokyo.s.is.www_tsujii.tools.enju.EnjuToken;
import uk.ac.nactem.uima.cas.bionlpst.Attribute;
import uk.ac.nactem.uima.cas.bionlpst.Entity;
import uk.ac.nactem.uima.cas.bionlpst.Event;
import uk.ac.nactem.uima.cas.semantic.EventParticipant;
import uk.ac.nactem.uima.cas.semantic.NamedEventParticipant;

public class UncertaintyOnPathwayStudio extends JCasAnnotator_ImplBase {

	
	private class EventStr{
		public String type = "";
		public String polarity="unknown";
		public boolean negation = false;
		public String primaryArg;
		public String secondaryArg;
		public String tertiaryArg = "";

		public boolean equals(EventStr event, Map<String,Set<String>> aliasmap){
			if(event.type.equals(this.type) || !relationMatch){
				if(type.contains("binding")){
					if((agree(event.primaryArg,this.primaryArg, aliasmap) && agree(event.secondaryArg,this.secondaryArg, aliasmap))||
							(agree(event.primaryArg,this.secondaryArg, aliasmap) && agree(event.secondaryArg,this.primaryArg, aliasmap))){
						if(polarityAgreement(event))
							return true;
					}
				}
				else{
					if(agree(event.primaryArg,this.primaryArg, aliasmap) && agree(event.secondaryArg,this.secondaryArg, aliasmap)){
						return true;
					}
				}
			}
			return false;
		}

		private boolean polarityAgreement(EventStr event) {
			if (event.getPolarity().equals("positive") || event.getPolarity().equals("unknown")){ 
				if((this.polarity.equals("positive") || this.polarity.equals("unknown")) && event.getNegation()==this.getNegation()){
					return true;
				}
				else if(this.polarity.equals("negative") && event.getNegation()!=this.getNegation()){
					return true;
				}
			}
			else if (event.getPolarity().equals("negative")){
				if(this.polarity.equals("negative") && event.getNegation()==this.getNegation()){
					return true;
				}
				else if (!event.getPolarity().equals(this.getPolarity()) && event.getNegation()!=this.getNegation()){
					return true;
				}
			}
			return false;
		}

		private String getPolarity() {
			return polarity;
		}

		private boolean agree(String arg1, String arg2, Map<String, Set<String>> aliasmap) {
			if(arg1!=null && arg2!=null){
				Set<String> aliases1 = aliasmap.get(arg1.toLowerCase().replace("-", ""));
				Set<String> aliases2 = aliasmap.get(arg2.toLowerCase().replace("-", ""));
				return agree(arg1,arg2,aliases1,aliases2);
			}
			else{
				return false;
			}
		}

		private boolean agree(String arg1, String arg2, Set<String>arg1Aliases, Set<String>arg2Alsiases) {
			if(arg1!=null && arg2!=null){
				String arg1l = arg1.toLowerCase().replace("-", "");
				String arg2l = arg2.toLowerCase().replace("-", ""); 			
				if(arg1l.equals(arg2l)){
			//		System.out.println("SAME : " + arg1l + " - " + arg2l);
					return true;
				}
				if(arg1Aliases!=null)
					if(arg1Aliases.contains(arg2l)){
				//		System.out.println("Found in aliases : " + arg1Aliases.size() + " - " + arg2l);
						return true;
					}
				if(arg2Alsiases!=null)
					if(arg2Alsiases.contains(arg1l)){
				//		System.out.println("Found in aliases : " + arg2Alsiases.size() + " - " + arg1l);
						return true;
					}
			}
			return false;
		}

		public void setPolarity(String string){
			string = string.toLowerCase();
			if(string.equals("negative") || string.equals("positive"))
				polarity = string;
			else{
				polarity = "unknown";
				System.out.println("WARNING: No valid polarity value for this event. Polarity can only be positive or negative. "
						+ "Set to unknown for now.");
			}
		}

		public void setNegation(){
			negation = true;
		}

		public boolean getNegation(){
			return negation;
		}

		public void setType(String string) {
			type = string;
		}

		public void setPArg(Annotation target) {
			if(getArgString(target,0)!=null)
				if(getArgString(target,0).length()>0)
					primaryArg = getArgString(target,0).toLowerCase().replace("-", "");

		}
		public void setSArg(Annotation target) {
			if(getArgString(target,0)!=null)
				if(getArgString(target,0).length()>0)
					secondaryArg = getArgString(target,0).toLowerCase().replace("-", "");

		}
		private String getArgString(Annotation arg, int level) {
			String argument = "";
			if(arg!=null)
				if(arg.getType()!=null)
					if(arg.getType().getName().contains("Entity")){
						argument = ((Entity)arg).getCoveredText().toLowerCase();
					}
					else if(arg.getType().getName().contains("Event")){
						Event event = (Event) arg;
						EventStr nestedEvent = getEventStrGen(event);
						argument = nestedEvent.getFlatStringFormat();
					}
			return argument;
		}

		private String getFlatStringFormat() {
			if(primaryArg!=null && secondaryArg!=null){
				return secondaryArg;
			}
			else if(primaryArg!=null){
				return primaryArg;
			}
			else if(secondaryArg!=null){
				return secondaryArg;
			}
			else
				return "";
		}

		private String getStringFormat() {
			if(primaryArg!=null && secondaryArg!=null){
				return "E<"+type+">"+"P<"+primaryArg+">"+"S<"+secondaryArg+">";
			}
			else if(primaryArg!=null){
				return "E<"+type+">"+"P<"+primaryArg+">"+"S<"+"null"+">";
			}
			else if(secondaryArg!=null){
				return "E<"+type+">"+"P<"+"null"+">"+"S<"+secondaryArg+">";
			}
			else
				return "E<"+type+">"+"P<"+"null"+">"+"S<"+"null"+">";
		}

		public String getPArg() {	
			return primaryArg;
		}

		public String getSArg() {	
			return secondaryArg;
		}
		public boolean isValid() {
			if(type!=null && primaryArg!=null && secondaryArg!=null && type.length()>0)
				return true;
			else
				return false;
		}

		public void setPArg(String participant) {
			if(participant!=null)
				primaryArg = participant.toLowerCase().replace("-", "");

		}

		public void setSArg(String participant) {
			if(participant!=null)
				secondaryArg = participant.toLowerCase().replace("-", "");
		}

		public Object getTArg() {
			return tertiaryArg;
		}

		public void setTArg(Annotation target) {
			if(getArgString(target,0)!=null)
				if(getArgString(target,0).length()>0)
					tertiaryArg = getArgString(target,0).toLowerCase().replace("-", "");
		}




		public boolean overlapsWith(EventStr eventStr, Map<String, Set<String>> entityMap) {
		//	System.out.println("CZDD: Checking overlap");
			if(eventStr.type.equals(this.type) || !relationMatch){
				if(type.contains("binding")){
					if(agree(eventStr.primaryArg,this.primaryArg, entityMap) || agree(eventStr.secondaryArg,this.secondaryArg, entityMap)||
							agree(eventStr.primaryArg,this.secondaryArg, entityMap) || agree(eventStr.secondaryArg,this.primaryArg, entityMap)){
						if(polarityAgreement(eventStr))
							return true;
					}
				}
				else{
					if(agree(eventStr.primaryArg,this.primaryArg, entityMap) || agree(eventStr.secondaryArg,this.secondaryArg, entityMap)){
						return true;
					}
				}
			}
			return false;
		}

		public String getNonOverlapping(EventStr eventStr, Map<String, Set<String>> entityMap) {
		//	System.out.println("CZDD: Get non overlapping");
			if(eventStr.type.equals(this.type) || !relationMatch){
				if(!agree(eventStr.primaryArg,this.primaryArg, entityMap) || !agree(eventStr.secondaryArg,this.primaryArg, entityMap)){
					return this.primaryArg;
				}
				else if(!agree(eventStr.primaryArg,this.secondaryArg, entityMap) || !agree(eventStr.secondaryArg,this.secondaryArg, entityMap)){
					return this.secondaryArg;
				}
			}
	//		System.out.println("CZDD: "+ this.secondaryArg + " - " + this.primaryArg);
			return null;
		}

	}

	public static final String PARAM_NAME_OUTFILE = "OutputFile";
//	public static final String PARAM_NAME_OUTFILE2 = "OutputFileTemp";
	public static final String PARAM_NAME_INFILE = "InputFile";
	public static final String PARAM_NAME_REL_MATCH = "RelationMatch";
	public static final String PARAM_NAME_NAME = "InteractionNameIdx";
	public static final String PARAM_NAME_TYPE = "InteractionTypeIdx";
	public static final String PARAM_NAME_POL = "InteractionPolarityIdx";
	public static final String PARAM_NAME_ALIAS = "AliasesIdx";
//	public static final String PARAM_NAME_DEPENDENCY = "DepDepth";
	
	Map<String,Set<String>> entityMap = new HashMap<String,Set<String>>();
	Map<String,Attribute> eventAttributeMap = new HashMap<String,Attribute>();
	Map<String,List<Integer>> uncertaintyMap = new HashMap<String, List<Integer>>();
	Map<String,Set<String>> modelPassages = new HashMap<String, Set<String>>();
	Map<String,Double> interactionUncertainty = new HashMap<String, Double>();
	Map<String,EventStr> eventMap = new HashMap<String, EventStr>();
	Map<String, String> interactionMap = new HashMap<String, String>();
	Map<String, Integer> interactionUncertaintyCount = new HashMap<String,Integer>();
	Map<String, Integer> interactionCount = new HashMap<String,Integer>();
	Map<String, List<Integer>> 	 uncertaintiesInteractionMap = new HashMap<String, List<Integer>>();
	public File outFile, inFile ;
	private int dependencyDepth;
	
	private int polarityTypeIdx, aliasIdx, interactionTypeIdx, nameIdx, pmids = 54; //indexes of the tsv file
	private HashSet<String> stopwords;
	private Boolean relationMatch;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		polarityTypeIdx = (Integer) aContext.getConfigParameterValue(PARAM_NAME_POL);
		nameIdx = (Integer) aContext.getConfigParameterValue(PARAM_NAME_NAME);
		interactionTypeIdx = (Integer) aContext.getConfigParameterValue(PARAM_NAME_TYPE);
		aliasIdx = (Integer) aContext.getConfigParameterValue(PARAM_NAME_ALIAS);
		outFile = new File((String) aContext.getConfigParameterValue(PARAM_NAME_OUTFILE));
		//outFile2 = new File((String) aContext.getConfigParameterValue(PARAM_NAME_OUTFILE2));
		inFile = new File((String) aContext.getConfigParameterValue(PARAM_NAME_INFILE));
		dependencyDepth = 3;
				//Integer.parseInt((String)aContext.getConfigParameterValue(PARAM_NAME_DEPENDENCY));
		relationMatch = (Boolean) aContext.getConfigParameterValue(PARAM_NAME_REL_MATCH);
		parseModel();
		initialiseStopWords();//for enju dependencies
		
	}

	private void initialiseStopWords() {
		stopwords = new HashSet<String>();
		stopwords.add("and");
		stopwords.add("or");
		stopwords.add("of");
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> doc = jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator();
		SourceDocumentInformation paper = (SourceDocumentInformation)doc.next();
		String filename = paper.getUri();
		String interactionName = filename.substring(filename.lastIndexOf("/")+1,filename.lastIndexOf("_"));
		EventStr interaction = eventMap.get(interactionName);
		if(interaction==null){
			System.out.println("No interaction corresponding to this file found : " + interactionName);
		}
		//populate the event attribute map
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
			}
		}

		List<Integer> uncertainties = uncertaintiesInteractionMap.get(interactionName);

		if(uncertainties == null){
			uncertainties = new ArrayList<Integer>(); 
		}
		FSIterator<Annotation> sentenceIterator = jcas.getAnnotationIndex(EnjuSentence.type).iterator();
		while (sentenceIterator.hasNext()) {
			EnjuSentence sentence = (EnjuSentence) sentenceIterator.next();
			//Iterate over all events of the sentence and calculate the additional features
			FSIterator<Annotation> eventIterator = jcas.getAnnotationIndex(Event.type).subiterator(sentence);
			while (eventIterator.hasNext()) {
				Event eventTM = (Event) eventIterator.next();
				EventStr eventStr = getEventStrGen(eventTM);
			
				if(eventStr.isValid()){	
					//if event is in model
					if(compatible(interactionName, eventStr)){
			
						int localUncertainty;
						Attribute uncertaintyAttribute = eventAttributeMap.get(eventTM.getId()+ ":Uncertainty");
			
						if(uncertaintyAttribute.getAttributeValue().equals("Uncertain"))
							localUncertainty = 0;
						else{
							localUncertainty = 1;
						}
						Set<String> passages = modelPassages.get(interactionName);
						if(passages == null){
							passages = new HashSet<String>();
						}
						passages.add(filename);
						modelPassages.put(interactionName, passages);
						uncertainties.add(localUncertainty);
						break; //max 1 corroborating event per sentence
					}
				}
				else{
					if(compatibleAlmost(interactionName, eventStr)){
				//if event is in model
						if(expandable(eventTM,sentence,interactionName, jcas)){
				
							int localUncertainty;
							Attribute uncertaintyAttribute = eventAttributeMap.get(eventTM.getId()+ ":Uncertainty");
				
							if(uncertaintyAttribute.getAttributeValue().equals("Certain"))
								localUncertainty = 1;
							else{
								localUncertainty = 0;
							}
							Set<String> passages = modelPassages.get(interactionName);
							if(passages == null){
								passages = new HashSet<String>();
							}
							passages.add(filename);
							modelPassages.put(interactionName, passages);
							uncertainties.add(localUncertainty);
							break; //max 1 corroborating event per sentence
						}
					}
					else{
					//	System.out.println("CZDD: Invalid Event; " + eventTM.getId() + " ; " + eventStr.getStringFormat() +" ; " +interactionName);
					}
				}
			}
		}
		uncertaintiesInteractionMap.put(interactionName, uncertainties);
	}


	private boolean expandable(Event eventTM, EnjuSentence sentence, String interaction, JCas jcas) {
		String interactionFull = interactionMap.get(interaction);
	//	System.out.println("CZ4IOB: for "+ interaction + "the full line is " + interactionFull);
		String [] fields = interactionFull.split("\t");
		String polarity = fields[polarityTypeIdx];
		String type = fields[interactionTypeIdx];
		String interactionName = fields[nameIdx];
		EventStr eventForm = parseInteraction(interactionName, polarity, type);
		EventStr eventStr = getEventStrGen(eventTM);
		String argument = eventForm.getNonOverlapping(eventStr, entityMap);
		if(isMatched(argument, eventTM, sentence, entityMap, jcas)){
			return true;
		}

		return false;
	}

	private boolean isMatched(String argument, Event eventTM, EnjuSentence sentence,
			Map<String, Set<String>> entityMap2, JCas jcas) {
		FSIterator<Annotation> tokenIterator = jcas.getAnnotationIndex(EnjuToken.type).subiterator(sentence);
		ArrayList<EnjuToken> tokens2match = new ArrayList<EnjuToken>();
		ArrayList<EnjuToken> alltokens = new ArrayList<EnjuToken>();
		String triggerID="";
		while (tokenIterator.hasNext()) {
			EnjuToken token = (EnjuToken) tokenIterator.next();
	//		System.out.println("CZDD2: "+ token.getCoveredText());
			if(token.getCoveredText()!=null && argument!=null){
				String name = token.getCoveredText().toLowerCase().replace("-", "");
	//			System.out.println("CZDD2: r :"+ name);
				String intName = argument.toLowerCase().replace("-", ""); 
	//			System.out.println("CZDD2: int :"+ intName);
				if(intName.equalsIgnoreCase(name)){ //TODO
	//				System.out.println("CZDD2: match :"+ name + " - " + intName);
					tokens2match.add(token);
				}
				else if(entityMap2.get(intName)!=null){
					if(entityMap2.get(intName).contains(name)){
	//					System.out.println("CZDD2: alias match :"+ name + " - " + intName);
						tokens2match.add(token);
					}
				}
				else if(entityMap2.get(intName)==null){
	//				System.out.println("CZDD2: NULL alias mapping for :" + intName + " - " + argument);
				}
				if(token.getBegin() <= eventTM.getBegin() && token.getEnd() >= eventTM.getEnd())
					triggerID = token.getId();
			}
			alltokens.add(token);
		}

		if(!tokens2match.isEmpty()){
	//		System.out.println("CZDD2: found matching tokens :");
			HashMap<String, Set<String>> deps = getDeps(alltokens);
			EnjuToken matchedToken = null;
			int minDist = Integer.MAX_VALUE;
			for(EnjuToken token2match : tokens2match){
				int dist = getMinimumDepth(token2match, eventTM, alltokens, deps, triggerID);
				if(dist < minDist){
					minDist = dist;
					matchedToken = token2match;
				}
			}
			
			if(matchedToken !=null && minDist<=dependencyDepth){
				return true;
			}
		}

		return false;
	}

	private HashMap<String, Set<String>> getDeps(ArrayList<EnjuToken> alltokens) {
		HashMap<String, Set<String>> map = new HashMap<String, Set<String>>();
		for(int i = 0; i <alltokens.size(); i++){
			EnjuToken token = alltokens.get(i);

//			System.out.println("CZDD2: getDeps :" + token.getCoveredText());
			Set<String> argTokens = getArgs(token);
			Set<String> tokenDeps = map.get(token.getId());
			if(tokenDeps==null){
				tokenDeps = new HashSet<String>();
			}
			if(argTokens!=null){
				tokenDeps.addAll(argTokens);
				map.put(token.getId(), tokenDeps);
				if(token.getCoveredText()!=null){
					if(stopwords.contains(token.getCoveredText().toLowerCase())){

						for(String tokenID : argTokens){
							Set<String> tokenIDs = map.get(tokenID);
							if(tokenIDs == null){
								tokenIDs = new HashSet<String>();
							}
							tokenIDs.addAll(argTokens);
							tokenIDs.remove(tokenID);
							map.put(tokenID, tokenIDs);

						}
					}
					else {
						for(String tokenID : argTokens){
							Set<String> tokenIDs = map.get(tokenID);
							if(tokenIDs == null){
								tokenIDs = new HashSet<String>();
							}
							tokenIDs.add(token.getId());
							map.put(tokenID, tokenIDs);
						}
					}
				}
				else {
					for(String tokenID : argTokens){
						Set<String> tokenIDs = map.get(tokenID);
						if(tokenIDs == null){
							tokenIDs = new HashSet<String>();
						}
						tokenIDs.add(token.getId());
						map.put(tokenID, tokenIDs);
					}
				}
			}
		}
		return map;
	}

	private Set<String> getArgs(EnjuToken token) {
		Set<String>	set = new HashSet<String>();
		//arg1
		EnjuConstituent arg1 = token.getArg1();
		if(arg1!=null){
			boolean found = false;
			while(!found && arg1!=null){
				Annotation h = arg1.getHead();
				if(h instanceof EnjuToken){
					String id = ((EnjuToken) h).getId();
					set.add(id);
					found = true;
				}
				else {
					arg1 = (EnjuConstituent) h;
				}
			}
		}
		EnjuConstituent arg2 = token.getArg1();
		if(arg2!=null){
			boolean found = false;
			while(!found && arg2!=null){
				Annotation h = arg2.getHead();
				if(h instanceof EnjuToken){
					String id = ((EnjuToken) h).getId();
					set.add(id);
					found = true;
				}
				else {
					arg2 = (EnjuConstituent) h;
				}
			}
		}
		return set;
	}

	private int getMinimumDepth(EnjuToken token, Event eventTM, ArrayList<EnjuToken> alltokens, HashMap<String, Set<String>> deps, String triggerID) {
		String tokID = token.getId();
		if(triggerID.length()>0){
			int minLength = getCount(tokID,triggerID, deps, 0);
			return minLength;
		}
		return Integer.MAX_VALUE;
	}

	private int getCount(String tokID, String triggerID, HashMap<String, Set<String>> deps, int i) {
	//	System.out.println("CZDD : "+ tokID + " - " + triggerID + " = " + i);
		int length = i+1;

		if (length>10 || deps.get(tokID).contains(triggerID)){
			return length;
		}
		else{
			int olength = Integer.MAX_VALUE;
			int tlength;
			for(String id : deps.get(tokID)){
				tlength = getCount(id, triggerID, deps, length);
				if(tlength<olength){
					olength=tlength;
				}
			}
			return olength;
		}
	}

	private boolean compatibleAlmost(String interaction, EventStr eventStr) {
	//	System.out.println("CZDD: Checking partial compatibility");
		String interactionFull = interactionMap.get(interaction);
	//	System.out.println("CZ4IOB: for "+ interaction + "the full line is " + interactionFull);
		String [] fields = interactionFull.split("\t");
		String polarity = fields[polarityTypeIdx];
		String type = fields[interactionTypeIdx];
		String interactionName = fields[nameIdx];
		EventStr eventForm = parseInteraction(interactionName, polarity, type);

		if(eventForm.overlapsWith(eventStr, entityMap)){
			return true;
		}

		return false;
	}


	private String getInvalidStr(Event event) {
		String invalid = "";
		invalid += "<E:"+event.getName()+">";
		FSArray participants = event.getParticipants();
		if(event.getName()!=null){
			for(int i =0; i < participants.size(); i++){
				NamedEventParticipant participant = (NamedEventParticipant) participants.get(i);
				Annotation target = (Annotation) participant.getTarget();
				invalid+="<"+participant.getName()+":"+target.getCoveredText()+">";
			}
		}
		return invalid;
	}

	public void collectionProcessComplete() throws AnalysisEngineProcessException {

		for(String interaction : uncertaintiesInteractionMap.keySet()){
	//		System.out.println("CZ4UE: Checking interaction: " + interaction);
			String interactionLine = interactionMap.get(interaction);
			if(interactionLine!=null){		
				String intName = interactionLine.split("\t")[nameIdx];
				List<Integer> uncertainty = uncertaintiesInteractionMap.get(interaction);
				Double overallUncertainty = getUncertainty(uncertainty);
				Integer uncertaintyCount = getUncertaintyCount(uncertainty);
				interactionUncertainty.put(intName, overallUncertainty);
				interactionUncertaintyCount.put(intName, uncertaintyCount);
				interactionCount.put(intName, uncertainty.size());
			}
		}
		addUncertaintyToModel();
		/*
		try{
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile2));	
		for (String interaction : modelPassages.keySet()){
			Set<String> pass = modelPassages.get(interaction);
			for(String p : pass){
				writer.write(p);
				writer.newLine();
			}
		}
		}catch(IOException e){
			e.printStackTrace();
		}
		*/
		super.collectionProcessComplete();
	}

	private Integer getUncertaintyCount(List<Integer> list) {
		int count = 0;
		for(int i =0; i<list.size(); i++){
			int un = list.get(i);
			if(un==0){
				count ++;
			}
		}

		return count;
	}

	private void addUncertaintyToModel() {
		int count=1;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			String line;

			while((line=reader.readLine())!=null){
				String intName = line.split("\t")[nameIdx];
				Double uncertainty = interactionUncertainty.get(intName);
				Integer uncertaintyCount = interactionUncertaintyCount.get(intName);
				Integer intCount = interactionCount.get(intName);
				if(uncertainty!=null){
					line += "\t"+uncertainty.toString(); 

				}
				if(uncertaintyCount!=null && intCount!=null){
					double uncertaintyPercent = 0.0;
					if(intCount==0){
						uncertaintyPercent = 0.0;
					}
					else{
						uncertaintyPercent = (double) (uncertaintyCount/intCount);
					}
					uncertaintyPercent = (double)uncertaintyPercent*100;
					uncertaintyPercent = uncertaintyPercent*100;
					uncertaintyPercent = (double)((int) uncertaintyPercent);
					uncertaintyPercent = uncertaintyPercent /100;
					
					line += "\t"+((Double)uncertaintyPercent).toString() + "%"; 
				}
/*
				if(intCount!=null){
					line += "\t"+intCount.toString(); 
				}
*/
				if(count==1){
					//line +="\tUncertainty From Original Evidence \t Uncertain Events Count \t Mapped Events Count \t Paper IDs for uncertainty";
					line +="\t Uncertainty Score \t Uncertain Events Percentage";
				}
				count++;
				writer.write(line);
				writer.newLine();
			}
			reader.close();
			writer.close();
		} catch (IOException e) {
			System.out.println("While parsing line :" + count);
			e.printStackTrace();
		}
	}

	private double getUncertainty(List<Integer> list) {
		double uncertainty = 1.0;
		for(int i =0; i<list.size(); i++){
			int un = list.get(i);
			if(un==1){
				//	uncertainty += 0.5*(1/(double)(i+1));
			}
			else if(un==0){
				uncertainty -= 0.5*(1/(double)(i+1));
			}
		}
		if(uncertainty<=0.0){
			uncertainty = 0.1;
		}
		if(uncertainty>1.0){
			uncertainty = 1.0;
		}
		return uncertainty;
	}

	private boolean compatible(String interaction, EventStr event) {
		String interactionFull = interactionMap.get(interaction);
	//	System.out.println("CZ4IOB: for "+ interaction + "the full line is " + interactionFull);
		String [] fields = interactionFull.split("\t");
		String polarity = fields[polarityTypeIdx];
		String type = fields[interactionTypeIdx];
		String interactionName = fields[nameIdx];
		EventStr eventForm = parseInteraction(interactionName, polarity, type);
	//	System.out.println(event.getStringFormat() + " - " + eventForm.getStringFormat());
		if(event.equals(eventForm,entityMap)){
			return true;
		}
		return false;
	}


	private EventStr getEventStrGen(Event event){
		ArrayList<NamedEventParticipant> argumentQueue = new ArrayList<NamedEventParticipant>();
		EventStr ev = new EventStr();
		if(event.getNegation())
			ev.setNegation();
		FSArray participants = event.getParticipants();
		if(event.getName()!=null){
			String eventType = event.getName().toLowerCase();

			for(int i =0; i < participants.size(); i++){
				NamedEventParticipant participant = (NamedEventParticipant) participants.get(i);
				if(participant!=null){
					Annotation target = (Annotation) participant.getTarget();
					if(participant.getName().contains("Theme") && ev.getPArg()==null && target!=null){
						ev.setPArg(target);
					}
					else if(participant.getName().contains("Cause") || ev.getPArg()!=null && target!=null){
						ev.setSArg(target);
					}
					else if(ev.getPArg()!=null && ev.getSArg()!=null && target!=null){
						ev.setTArg(target);
					}
					else{
						argumentQueue.add(participant);
					}
				}
			}
			//there are roles that are not Theme or Cause. For now we just treat them in random order. To be revised
			for(NamedEventParticipant participant: argumentQueue){
				if(participant!=null){
					Annotation target = (Annotation) participant.getTarget();
					if(ev.getPArg()==null && target!=null){
						ev.setPArg(target);
					}
					else if(ev.getSArg()==null && target!=null){
						ev.setSArg(target);
					}
					else if(ev.getTArg()==null && target!=null){
						ev.setTArg(target);
					}
				}
			}
			//REGULATION
			if(eventType.contains("regulation") || eventType.contains("inhibition") || eventType.contains("activation") ){
				if(eventType.contains("negative") || eventType.contains("in")){
					ev.setType("regulation");
					ev.setPolarity("negative");
				}
				else{
					ev.setType("regulation");
					ev.setPolarity("positive");
				}
			}
			//BINDING
			else if(eventType.contains("binding")){
				ev.setType("binding");
			}
			else if(eventType.contains("expression")){
				ev.setType("regulation"); //TODO change?
				ev.setPolarity("positive");//TODO leave to default?
			}
			else if(eventType.contains("pathway")){
				ev.setType("pathway");
			}
			else if(eventType.contains("phosphorylation")){
				ev.setType("phosphorylation");
			}
			else if(eventType.contains("process") || eventType.contains("mutation") || eventType.contains("translocation")){
				ev.setType("process");
			}
			else{
				ev.setType("unknown");
			}
		}
		return ev;
	}


	private EventStr getEventStr(Event event){
		EventStr ev = new EventStr();
		if(event.getNegation())
			ev.setNegation();
		FSArray participants = event.getParticipants();
		if(event.getName()!=null){
			String eventType = event.getName().toLowerCase();

			if(participants.size() >= 2  ){
				//REGULATION

				if(eventType.contains("regulation") || eventType.contains("inhibition") || eventType.contains("activation") ){
					if(eventType.contains("negative") || eventType.contains("in")){
						ev.setType("regulation");
						ev.setPolarity("negative");
					}
					else{
						ev.setType("regulation");
						ev.setPolarity("positive");
					}
					for(int i =0; i < participants.size(); i++){
						NamedEventParticipant participant = (NamedEventParticipant) participants.get(i);
						Annotation target = (Annotation) participant.getTarget();
						if(participant.getName().contains("Theme") && ev.getPArg()==null){
							ev.setPArg(target);
						}
						else if(participant.getName().contains("Cause") || ev.getPArg()!=null ){
							ev.setSArg(target);
						}
						else if(ev.getPArg()!=null && ev.getSArg()!=null){
							ev.setTArg(target);
						}
					}
				}

				//BINDING
				else if(eventType.contains("binding")){
					ev.setType("binding");
					for(int i =0; i < participants.size(); i++){
						NamedEventParticipant participant = (NamedEventParticipant) participants.get(i);
						Annotation target = (Annotation) participant.getTarget();
						if(participant.getName().contains("Theme") && ev.getPArg()==null){
							ev.setPArg(target);
						}
						else if(participant.getName().contains("Cause") || ev.getPArg()!=null ){
							ev.setSArg(target);
						}
						else if(ev.getPArg()!=null && ev.getSArg()!=null){
							ev.setTArg(target);
						}
					}
				}
				else if(eventType.contains("expression")){
					ev.setType("regulation"); //TODO change?
					ev.setPolarity("positive");//TODO leave to default?
					for(int i =0; i < participants.size(); i++){
						NamedEventParticipant participant = (NamedEventParticipant) participants.get(i);
						Annotation target = (Annotation) participant.getTarget();
						if(participant.getName().contains("Theme") && ev.getPArg()==null){
							ev.setPArg(target);
						}
						else if(participant.getName().contains("Cause") || ev.getPArg()!=null){
							ev.setSArg(target);
						}
						else if(ev.getPArg()!=null && ev.getSArg()!=null){
							ev.setTArg(target);
						}
					}
				}
				else if(eventType.contains("pathway")){
					ev.setType("pathway");
					for(int i =0; i < participants.size(); i++){
						NamedEventParticipant participant = (NamedEventParticipant) participants.get(i);
						Annotation target = (Annotation) participant.getTarget();
						if(participant.getName().contains("Theme") && ev.getPArg()==null){
							ev.setPArg(target);
						}
						else if(participant.getName().contains("Cause") || ev.getPArg()!=null ){
							ev.setSArg(target);
						}
						else if(ev.getPArg()!=null && ev.getSArg()!=null){
							ev.setTArg(target);
						}
					}
				}
				else if(eventType.contains("phosphorylation")){
					ev.setType("phosphorylation");
					for(int i =0; i < participants.size(); i++){
						NamedEventParticipant participant = (NamedEventParticipant) participants.get(i);
						Annotation target = (Annotation) participant.getTarget();
						if(participant.getName().contains("Theme") && ev.getPArg()==null){
							ev.setPArg(target);
						}
						else if(participant.getName().contains("Cause") || ev.getPArg()!=null ){
							ev.setSArg(target);
						}
						else if(ev.getPArg()!=null && ev.getSArg()!=null){
							ev.setTArg(target);
						}
					}
				}
				else if(eventType.contains("process") || eventType.contains("mutation") || eventType.contains("translocation")){
					ev.setType("process");
					for(int i =0; i < participants.size(); i++){
						NamedEventParticipant participant = (NamedEventParticipant) participants.get(i);
						Annotation target = (Annotation) participant.getTarget();
						if(participant.getName().contains("Theme") && ev.getPArg()==null){
							ev.setPArg(target);
						}
						else if(participant.getName().contains("Cause") || ev.getPArg()!=null ){
							ev.setSArg(target);
						}
						else if(ev.getPArg()!=null && ev.getSArg()!=null){
							ev.setTArg(target);
						}
					}
				}
			}
			else if(participants.size()>0){
			//	System.out.println(eventType + " - " + ((Annotation) ((EventParticipant) participants.get(0)).getTarget()).getCoveredText());

			}
		}
		return ev;

	}

	public void parseModel(){
		File modelFile  = inFile;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(modelFile));
			String line;
			reader.readLine(); //title line
			reader.readLine();//network line
			while((line=reader.readLine())!=null){
				String [] fields = line.split("\t");
				if(fields.length>1){
					if(fields[4].length()>0){
						Set<String> aliasSet = new TreeSet<String>();
						if(fields.length > aliasIdx+1){
							String [] aliases = fields[aliasIdx].split(";");
							for(String alias: aliases){
								aliasSet.add(alias.toLowerCase().replace("-", ""));
							}
						}
						entityMap.put(fields[nameIdx].toLowerCase().replace("-", ""), aliasSet);
					}
					else if(fields[5].length()>0 && !line.startsWith("Name")){ //just making sure it is not the title line
						String interaction = fields[nameIdx];
						String polarity = fields[polarityTypeIdx];
						String type = fields[interactionTypeIdx];
						EventStr event = parseInteraction(interaction, polarity, type);
						if (event!=null){	
							String fileName =  interaction.replaceAll(":", "_");
							fileName = fileName.replaceAll("-", "_");
							fileName = fileName.replaceAll(" ", "_");
							fileName = fileName.replaceAll(">", "_");
							fileName = fileName.replaceAll("\\|", "_");
							fileName = fileName.replaceAll("\\+", "_"); 
							//System.out.println("THIS<"+fileName+">");
							interactionMap.put(fileName, line);
							eventMap.put(fileName, event);
						}
						else{
							System.out.println("Event not created:" + interaction);
						}
					}
				}
			}
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}


	private EventStr parseInteraction(String interaction, String polarity, String type) {
		String participantA = null, participantB = null;
		try{
			if(polarity.equals("") && (type.equals("Expression") || type.equals("PromoterBinding") || type.equals("DirectRegulation") 
					|| type.equals("Regulation") || type.equals("ProtModification") || type.equals("MolTransport"))){
				participantA = interaction.substring(interaction.indexOf(":")+2,interaction.indexOf("--->")-1);
				participantB = interaction.substring(interaction.indexOf("--->")+5);
			}
			else if(polarity.equals("positive") && (type.equals("Expression") || type.equals("PromoterBinding") || type.equals("DirectRegulation") 
					|| type.equals("Regulation") || type.equals("ProtModification") || type.equals("MolTransport"))){
				participantA = interaction.substring(interaction.indexOf(":")+2,interaction.indexOf("--+>")-1);
				participantB = interaction.substring(interaction.indexOf("--+>")+5);
			}
			else if(polarity.equals("negative") && (type.equals("Expression") || type.equals("PromoterBinding") || type.equals("DirectRegulation") 
					|| type.equals("Regulation") || type.equals("ProtModification") || type.equals("MolTransport"))){
				participantA = interaction.substring(interaction.indexOf(":")+2,interaction.indexOf("---|")-1);
				participantB = interaction.substring(interaction.indexOf("---|")+5);
			}
			else if(polarity.equals("") && type.equals("Binding") ){
				participantA = interaction.substring(interaction.indexOf(":")+2,interaction.indexOf("----")-1);
				participantB = interaction.substring(interaction.indexOf("----")+5);
			}
			else if(polarity.equals("") && type.equals("ChemicalReaction") ){
				participantA = interaction.substring(interaction.indexOf(":")+2,interaction.indexOf("->->")-1);
				participantB = interaction.substring(interaction.indexOf("->->")+5);
			}
			if(participantA!=null && participantB!=null){
				EventStr ev = new EventStr();
				ev.setPArg(participantB);
				ev.setSArg(participantA);
				switch (type){
				case "Expression":
					if(polarity.equals("negative")){
						ev.setType("regulation-");
					}
					else if(polarity.equals("positive")){
						ev.setType("regulation+");
					}
					else{
						ev.setType("regulation");
					}
					break;

				case "Regulation":
					if(polarity.equals("negative")){
						ev.setType("regulation-");
					}
					else if(polarity.equals("positive")){
						ev.setType("regulation+");
					}
					else{
						ev.setType("regulation");
					}
					break;
				case "DirectRegulation":
					ev.setType("regulation");
					if(polarity.equals("negative") || polarity.equals("negative")){
						ev.setPolarity(polarity);
					}
					break;
				case "Binding":
					ev.setType("binding");
					break;
				case "PromoterBinding": //TODO revise
					ev.setType("regulation");
					if(polarity.equals("negative") || polarity.equals("negative")){
						ev.setPolarity(polarity);
					}
					break;
				case "ChemicalReaction":
					ev.setType("unknown");
					break;
				case "ProtModification":
					ev.setType("phoshphorylation");
					if(polarity.equals("negative") || polarity.equals("negative")){
						ev.setPolarity(polarity);
					}
					break;
				case "MolTransport":
					ev.setType("transport");
					break;
				default:
					ev.setType("unknown");
					break;
				}
				//System.out.println(ev.getStringFormat());
				return ev;
			}
		}catch(StringIndexOutOfBoundsException e){
			System.out.println("CZ4IOB: IOB for: "+ interaction + "with polarity: " + polarity + "with type: "+ type);
			e.printStackTrace();
		}
		return null;
	}

}


