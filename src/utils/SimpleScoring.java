package utils;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.terrier.applications.batchquerying.TRECQuery;
import org.terrier.matching.FatQueryResultSet;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.querying.parser.Query;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.utility.ApplicationSetup;

import eval.Qrel;
import utils.Utils.QREL;

public class SimpleScoring {
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
	
	
	/* Initialize Simple model with index. Use 
	 * @param index_path : initialize index 
	 * @param prefix : language prefix for index 
	 * with location of index created using bash script.
	 */
	public SimpleScoring(String index_path, String prefix) {
		
		// Load the index and collection stats
		try {
			index = Index.createIndex(index_path, prefix);
			
			System.out.println("Loaded index from path "+index_path+" "+index.toString());
			total_tokens = index.getCollectionStatistics().getNumberOfTokens();
			total_documents = index.getCollectionStatistics().getNumberOfDocuments();
			System.out.println("Number of terms and documents in index "+total_tokens+" "+total_documents);
			
	
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public HashMap <String, Double> buildResultSet(String id, String query){
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
		
		final String metaIndexDocumentKey = ApplicationSetup.getProperty("trec.querying.outputformat.docno.meta.key", "filename");
		String doc_names [] = Utils.getDocnos(metaIndexDocumentKey, set, index);
		
		
		HashMap <String, Double> scores = new HashMap<String, Double>();
		for (int i = 0 ; i < doc_scores.length;i++){
			//System.out.println(doc_ids[i]+" "+doc_scores[i]);
			scores.put(doc_names[i], doc_scores[i]);
			if (i==10)
				break;
		}
			
		
		return scores;
		
	}
	public void closeIndex() {
		try {
			index.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String [] args) {
		
		
		System.setProperty("terrier.home", "/path/to/terrier-core-4.1");
		
		// Topic file path
		String topic_file_path = "/path/to/trec2013-topics.txt";
		
		// Index path
		String index_path = "/path/to/terrier-core-4.1/var/index/";
		
		// Qrel path
		String qrel_path = "/path/to/qrels.adhoc.txt";
		
		// Load the topics
		TRECQuery trec_topics = new TRECQuery(topic_file_path);
		
		// Load the qrels
		ArrayList <Qrel> qrels = Utils.loadQrels(qrel_path, QREL.ADHOC);
				
		// Initialize the scorer
		SimpleScoring scorer = new SimpleScoring(index_path, "data");
		
		int i = 0;
		
		while(trec_topics.hasNext())
		{
			String query = trec_topics.next();
			HashMap<String, Double> scores = scorer.buildResultSet(i+"", trec_topics.next());
			
			for (Map.Entry<String, Double> entry : scores.entrySet())
				System.out.println(i+"\t"+entry.getKey()+"\t"+entry.getValue());
			i++;
		}
		scorer.closeIndex();
		
		
	}
}
