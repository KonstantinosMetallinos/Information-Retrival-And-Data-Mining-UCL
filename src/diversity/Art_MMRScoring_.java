package diversity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.terrier.applications.batchquerying.TRECQuery;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;

import utils.Utils;

public class Art_MMRScoring_ {

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
	
	
	/* Initialize MMR model with index. Use 
	 * @param index_path : initialize index 
	 * @param prefix : language prefix for index (default = 'en')
	 * with location of index created using bash script.
	 */
	public Art_MMRScoring_(String index_path, String prefix) throws Exception {
		index = Index.createIndex(index_path, prefix);
		term_lexicon = index.getLexicon();
		
		System.out.println("Loaded index from path "+index_path+" "+index.toString());
		total_tokens = index.getCollectionStatistics().getNumberOfTokens();
		total_documents = index.getCollectionStatistics().getNumberOfDocuments();
		System.out.println("Number of terms and documents in index "+total_tokens+" "+total_documents);
	}
	
	
	public double getSimilarity(int document_id1, int document_id2) {
		HashMap<Integer, Integer> TF1 = Utils.docTF(document_id1, index);
		HashMap<Integer, Integer> TF2 = Utils.docTF(document_id2, index);
		
		Set<Integer> common_terms = new HashSet<Integer>();
		for(int key: TF1.keySet()) {
			if (TF2.containsKey(key))
				common_terms.add(key);
		}

		double nominator = 0.0;
		double denominator1 = 0.0;
		double denominator2 = 0.0;
		for(int key: common_terms) {
			nominator += TF1.get(key) * TF2.get(key);
			denominator1 += Math.pow(TF1.get(key), 2);
			denominator2 += Math.pow(TF2.get(key), 2);
		}
		return nominator / Math.sqrt(denominator1)*Math.sqrt(denominator2);
	}
	
	
	public double scoreDocumentWrtList(int document_id, String query, ArrayList <Integer> result_list) {
		ArrayList<Double> list = new ArrayList<Double>();
		for (int docID: result_list) {
			list.add(getSimilarity(document_id, docID));
		}
		Collections.sort(list);
		return list.get(list.size()-1);
	}
	
	public double scoreDocument(int document_id, String query, double lambda, double score) {
		return 0;
	}
	
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
			//System.out.println(doc_ids[i]+" "+doc_scores[i]);
			scores.put(doc_ids[i], doc_scores[i]);
			if (i==10)
				break;
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
	
	public static void main(String [] args) throws Exception {
		System.setProperty("terrier.home", "C:/Users/Maly/Documents/IRDA_ICA1/terrier-core-4.1"); 
		String topic_file_path = "C:/Users/Maly/Documents/IRDA_ICA1/trec2013-topics.txt"; 		// Topic file path
		String index_path = "C:/Users/Maly/Documents/IRDA_ICA1/terrier-core-4.1/var/index/";	// Index path
		
		TRECQuery trec_topics = new TRECQuery(topic_file_path);
		Art_MMRScoring_ scorer = new Art_MMRScoring_(index_path, "data");
		
		int i = 0;
		double lambda = 0.25, value=0;
		while(trec_topics.hasNext())
		{
			String query = trec_topics.next();
			HashMap<Integer, Double> scores = scorer.buildResultSet(i+"", trec_topics.next(), lambda);
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (int key: scores.keySet()) {
				list.add(key);
			}
			
			HashMap<Integer, Double> MMRScores = new HashMap<Integer, Double>();
			for (Map.Entry<Integer, Double> counter : scores.entrySet()) {
				//list.remove(entry.getKey());
				value = 0.0;
				value = lambda*counter.getValue() - (1-lambda)*scorer.scoreDocumentWrtList(counter.getKey(), query, list);
				MMRScores.put(counter.getKey(), value);
				System.out.println(i + " "+ counter.getKey() + " ---- " + value);
			}
			i++;
		}
		System.out.println("queries: " + i);
		scorer.closeIndex();
	}
}
