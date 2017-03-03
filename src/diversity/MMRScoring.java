package diversity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.terrier.applications.batchquerying.TRECQuery;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.utility.ApplicationSetup;



import eval.Qrel;
import utils.SimpleScoring;
import utils.Utils;
import utils.Utils.QREL;

public class MMRScoring {

	/* Terrier Index */
	Index index;
	
	/* Index structures*/
	/* list of terms in the index */
	Lexicon<String> term_lexicon = null;
	/* list of documents in the index */
	DocumentIndex doi = null;
	
	/* Collection statistics */
	long total_tokens;
	long total_documents;
	long distinct_tokens;

	
	/* Initialize MMR model with index. Use 
	 * @param index_path : initialize index 
	 * @param prefix : language prefix for index (default = 'en')
	 * with location of index created using bash script.
	 */
	public MMRScoring(String index_path, String prefix) {
		try {
			index = Index.createIndex(index_path, prefix);
			term_lexicon = index.getLexicon();
			
			System.out.println("Loaded index from path "+index_path+" "+index.toString());
			total_tokens = index.getCollectionStatistics().getNumberOfTokens();
			total_documents = index.getCollectionStatistics().getNumberOfDocuments();
			System.out.println("Number of terms and documents in index "+total_tokens+" "+total_documents);
			distinct_tokens = term_lexicon.numberOfEntries(); 
					
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	
	public double getSimilarity(int document_id1, int document_id2) {
		
		HashMap<Integer, Integer> termfreq1 = Utils.docTF(document_id1, index);
		HashMap<Integer, Integer> termfreq2 = Utils.docTF(document_id2, index);
		
		Set<Integer> common_terms = new HashSet<Integer>();
	
		for(int counter: termfreq1.keySet()){
			if(termfreq2.containsKey(counter)){common_terms.add(counter);}
		}
		
		if(common_terms.isEmpty()){return 0;}
		
		double nominator = 0.0;
		double denominator = 0.0;
		double temp = 0.0;
		
		for(int counter: common_terms){nominator 	+= (termfreq1.getOrDefault(counter, 0))*(termfreq2.getOrDefault(counter, 0));}
		for(int counter: common_terms){temp			+= (termfreq1.getOrDefault(counter, 0))*(termfreq1.getOrDefault(counter, 0));}
		for(int counter: common_terms){denominator 	+= (termfreq2.getOrDefault(counter, 0))*(termfreq2.getOrDefault(counter, 0));}
		
		denominator = Math.sqrt(denominator)*Math.sqrt(temp);
		
		return nominator/denominator;
		
	}
	
	
	public double scoreDocumentWrtList(int document_id, String query, ArrayList <Integer> result_list){
		
		ArrayList<Double> list = new ArrayList<Double>();
		
		for(int counter=0; counter<result_list.size();counter++ ){
			
			if(result_list.get(counter) == document_id){
				list.add(0.0);
			}else{
			list.add(getSimilarity(document_id,result_list.get(counter)));
			}
		}
		Collections.sort(list);
		
		return list.get(list.size() - 1);
	} 
	

	public double scoreDocument(int document_id, String query, double lambda){return 0;}  // -- not used at all
	
	
	public HashMap <Integer, Double> buildResultSet(String id, String query, double lambda) {
	
		// Just find documents and their posting list for the query. 
		
		// Create a search request object.
		Manager manager = new Manager(this.index);
		SearchRequest srq = manager.newSearchRequest(id, query);
		
		// Get the results using tfidf
		srq.addMatchingModel("Matching", "TF_IDF");
		manager.runPreProcessing(srq);
		manager.runMatching(srq);
		manager.runPostProcessing(srq);
		manager.runPostFilters(srq);
		
		ResultSet set = srq.getResultSet();
		int doc_ids [] = set.getDocids();
		double doc_scores [] = set.getScores();		
		
		HashMap <Integer, Double> scores = new HashMap<Integer, Double>();
		for (int i = 0 ; i < doc_scores.length;i++){
			scores.put(doc_ids[i], doc_scores[i]);
			if (i==99){break;}
		}
		
		return scores;
		
	}
		
	public void closeIndex() {
		try {
			index.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String [] args) throws FileNotFoundException, UnsupportedEncodingException {
		
		System.setProperty("terrier.home", "C:/Users/Maly/Documents/IRDA_ICA1/terrier-core-4.1"); 
		String topic_file_path = "C:/Users/Maly/Documents/IRDA_ICA1/trec2013-topics.txt"; 		// Topic file path
		String index_path = "C:/Users/Maly/Documents/IRDA_ICA1/terrier-core-4.1/var/index/";	// Index path
		String qrel_path = "C:/Users/Maly/Documents/IRDA_ICA1/qrels.adhoc.txt"; 				// Qrel path
		TRECQuery trec_topics = new TRECQuery(topic_file_path); 								// Load the topics
		ArrayList <Qrel> qrels = Utils.loadQrels(qrel_path, QREL.ADHOC); 						// Load the qrels		
		MMRScoring scorer = new MMRScoring(index_path, "data"); 								// Initialise the scorer
		
		int i = 1;
		double value1 = 0.0, value2 = 0.0, lambda1 = 0.25, lambda2 = 0.5;
		
		
		PrintWriter printout025 = new PrintWriter("MMR_TF_IDF_0_25.txt", "UTF-8");
		PrintWriter printout05 = new PrintWriter("MMR_TF_IDF_0_50.txt", "UTF-8");
		
		//printout05.println("Maximum Marginal Relevance using TF_IDF with lambda = 0.50");
		//printout025.println("Maximum Marginal Relevance using TF_IDF with lambda = 0.25");
		//printout05.println("Topic \t | \t Doc.ID  \t | \t Score");
		//printout025.println("Topic \t | \t Doc.ID  \t | \t Score");
		//printout025.println();
		//printout05.println();
		
		while(trec_topics.hasNext()){
			
			String query = trec_topics.next();
			HashMap<Integer, Double> scores = scorer.buildResultSet(i+"",query, lambda1);		
			ArrayList<Integer> list = new ArrayList<Integer>();
			
			for (int counter: scores.keySet()){list.add(counter);}

			HashMap<Integer, Double> temp1 = new HashMap<Integer, Double>();
			HashMap<Integer, Double> temp2 = new HashMap<Integer, Double>();
			Map<Integer, Double> MMRScores1 = new HashMap<Integer, Double>();
			Map<Integer, Double> MMRScores2 = new HashMap<Integer, Double>();
			
			
			for (Map.Entry<Integer, Double> counterscore : scores.entrySet()) {
				value1 = 0.0; value2 = 0.0; // set them =0 in case of "unexpected bugs" to receive 0 instead of duplicate scores
				value1 = lambda1*counterscore.getValue() - (1-lambda1)*scorer.scoreDocumentWrtList(counterscore.getKey(), query, list);
				value2 = lambda2*counterscore.getValue() - (1-lambda2)*scorer.scoreDocumentWrtList(counterscore.getKey(), query, list);
				temp1.put(counterscore.getKey(), value1);
				temp2.put(counterscore.getKey(), value2);
			}
			
			MMRScores1 = sortByValue(temp1);
			MMRScores2 = sortByValue(temp2);	
		//	for(int counter :MMRScores1.keySet()){printout025.println(i + " \t | \t "+ counter + " \t | \t " + MMRScores1.get(counter));}
		//	for(int counter :MMRScores2.keySet()){printout05.println(i + " \t | \t "+ counter + " \t | \t " + MMRScores2.get(counter));}
			for(int counter :MMRScores1.keySet()){printout025.println(i + " "+ counter + " " + MMRScores1.get(counter));}
			for(int counter :MMRScores2.keySet()){printout05.println(i + " "+ counter + " " + MMRScores2.get(counter));}
			
		//	printout025.println();
		//	printout05.println();
			System.out.println("-------------------------- Query "+i+" Done --------------------------");
			i++;
			
		}
		
		scorer.closeIndex();
		printout05.close();
		printout025.close();
		
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map ){ // credits http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
		List<Map.Entry<K, V>> list = new LinkedList<>( map.entrySet() );
		Collections.sort( list, new Comparator<Map.Entry<K, V>>(){
			@Override
			public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 ){return (o2.getValue()).compareTo( o1.getValue() );}
		} );

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list){result.put( entry.getKey(), entry.getValue() );}
		return result;
	}
	
}
