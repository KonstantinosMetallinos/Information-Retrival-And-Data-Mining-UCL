package diversity;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;

import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.utility.ApplicationSetup;

import utils.Utils;

import org.terrier.applications.batchquerying.TRECQuery;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set; 

public class PortfolioScoring {

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
	
	HashMap<Integer, HashMap<Integer, Integer>> docTermVector = new HashMap<Integer, HashMap<Integer, Integer>>();
	HashMap<Integer, Double> docMeanVector = new HashMap<Integer, Double>();

	
	public PortfolioScoring(String index_path, String prefix) {
		
		// Load the index and collection stats
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

	/* *
	Computes correlation between term vectors of 2 documents .
	 * */
	public double getCorrelation (int document_id, String query, int document_id2) 
	{
		HashMap<Integer, Integer> termfreq1 = Utils.docTF(document_id, index);
		HashMap<Integer, Integer> termfreq2 = Utils.docTF(document_id2, index);
		
		Set<Integer> all_terms = new HashSet<Integer>();
		for(int counter: termfreq1.keySet()){all_terms.add(counter);}
		for(int counter: termfreq2.keySet()){all_terms.add(counter);}
		
		double nominator = 0.0,denominator_prt1 = 0.0, denominator_prt2=0.0;
		double mean1 = computeMean(document_id,new ArrayList<Integer>()); 
		double mean2 = computeMean(document_id2,new ArrayList<Integer>()); 
		
		for(int counter: all_terms){
			nominator += (termfreq1.getOrDefault(counter, 0)- mean1)*(termfreq2.getOrDefault(counter, 0)- mean2);
			denominator_prt1 += Math.pow((termfreq1.getOrDefault(counter, 0)- mean1),2);
			denominator_prt2 += Math.pow((termfreq2.getOrDefault(counter, 0)- mean2),2);
		}
		
		double personscorrelation = nominator/(Math.sqrt(denominator_prt1)*Math.sqrt(denominator_prt2));
		
		return personscorrelation;
	}


	/* * Computes Variance * */
	public double computeVariance ( int document_id, ArrayList <Integer> result_list ){return 1;}


	/* * Computes Mean * */
	public double computeMean (int document_id, ArrayList < Integer > result_list ) 
	{
		HashMap<Integer, Integer>termfreq1 = Utils.docTF(document_id, index) ;		
		double mean = 0.0;

		for(int counter:termfreq1.keySet()){mean+=termfreq1.get(counter);}
		return mean/termfreq1.size();
	}
	
	public double scoreDocumentwrtList(int document_id, ArrayList<Integer> result_list, double b){
		
		int rank = result_list.size()+1;

		double score = 0.0;
		int i = 1;
		for(int document: result_list){
			score += 1.0/Math.pow(i, 2)*getCorrelation(document_id,"a" , document);
			i++;
		}
		score = -2*b*score;
		score += docMeanVector.get(document_id) - b/Math.pow(rank,2);
		return score;
	}
	
	
	
	
	/* * Computes score of document * */
	public double scoreDocument (int document_id , String query )
	{

		return 0;
	}


	/* * Iteratively build the result list .
	The input parameters are search query and
	discount factor ( e.g. 1/r ) that yields rank
	specific w i.e. wj and the
	parameter b responsible for adjusting risk .
	 * */
	public HashMap <String, Double> buildResultSet(String id, String query, double weight, double b){
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

		set.setResultSize(100);
		doc_ids = java.util.Arrays.copyOfRange(doc_ids, 0, Math.min(100, doc_ids.length));		  
		doc_scores = java.util.Arrays.copyOfRange(doc_scores, 0, Math.min(100, doc_ids.length));
				
		final String metaIndexDocumentKey = ApplicationSetup.getProperty("trec.querying.outputformat.docno.meta.key", "filename");
		String doc_names [] = Utils.getDocnos(metaIndexDocumentKey, set, index);
		

		HashMap<Integer, String> id_name = new HashMap<Integer, String>();
		for(int counter=0; counter<doc_ids.length; counter++){id_name.put(doc_ids[counter], doc_names[counter]);}
		
		docTermVector.clear();
		for(int counter: doc_ids){docTermVector.put(counter, Utils.docTF(counter, index));}
		
		//calculate documents mean
		if(doc_scores.length>0){
			double max = doc_scores[0];
			double min = doc_scores[doc_scores.length-1];
		
			docMeanVector.clear();
			for(int counter=0; counter<doc_ids.length; counter++){docMeanVector.put(doc_ids[counter], (doc_scores[counter]-min)/(max-min));}
		}
		
		
		
		ArrayList<Integer> chosen_docs = new ArrayList<Integer>();	
		LinkedHashMap<String, Double> scores  = new LinkedHashMap<String, Double>();	

		while (chosen_docs.size()< doc_ids.length){	
			double max_score = -9999.9;
			int bestDoc = 0;
			for(int doccounter: doc_ids){
				
				if (chosen_docs.contains(doccounter)) continue;
				double score = scoreDocumentwrtList(doccounter, chosen_docs, b);

				if (score>max_score){
					max_score = score;
					bestDoc = doccounter;
				}
			}
			chosen_docs.add(bestDoc);
			System.out.println(bestDoc+" "+max_score);
			scores.put(id_name.get(bestDoc), max_score);
		}
		return scores; 
		
	}

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException{
		
		System.setProperty("terrier.home", "C:/Users/Maly/Documents/IRDA_ICA1/terrier-core-4.1");
		TRECQuery trec_topics = new TRECQuery("C:/Users/Maly/Documents/IRDA_ICA1/trec2013-topics.txt");
	
		PortfolioScoring score = new PortfolioScoring("C:/Users/Maly/Documents/IRDA_ICA1/terrier-core-4.1/var/index", "data");
		
		PrintWriter printoutb4 = new PrintWriter("Port_TF_IDF_b4.txt", "UTF-8");
		PrintWriter printoutb_4 = new PrintWriter("Port_TF_IDF_b-4.txt", "UTF-8");
		//printoutb4.println("Portfolio using TF_IDF with beta = 4");
		//printoutb_4.println("Portfolio using TF_IDF with beta = -4");
		//printoutb4.println("Topic \t | \t Doc.ID  \t | \t Score");
		//printoutb_4.println("Topic \t | \t Doc.ID  \t | \t Score");
		//printoutb4.println("");
		//printoutb_4.println("");

		
		while(trec_topics.hasNext())
		{
			String query = trec_topics.next();
			String topic = trec_topics.getQueryId(); 

		//	HashMap<String, Double> scoresb4  = score.buildResultSet(topic,query,0.0,4.0);
			HashMap<String, Double> scoresb_4 = score.buildResultSet(topic,query,0.0,-4.0);

/*			for (Map.Entry<String, Double> counter : scoresb4.entrySet()){
				printoutb4.println("\t"+counter.getKey()+"\t"+counter.getValue());
			}*/
			for (Map.Entry<String, Double> counter : scoresb_4.entrySet()){
				printoutb_4.println("\t"+counter.getKey()+"\t"+counter.getValue());
			}
			//	printoutb4.println("");
			//	printoutb_4.println("");

		}
		
		printoutb4.close();
		printoutb_4.close();
		
	}
	
}
