package eval;

public class Result {
	  String document_id;
	  int document_rank;
	  double score;
	  
	  
	  // Add the constructors.

	  // Create getDocumentId and seDocumentId functions.
	  // Create getDocumentScore and seDocumentScore functions.
	  // Create getDocumentRank and seDocumentRank functions.
	  
	public Result(String document_id, int document_rank, double score) {
		
		//super();
		this.document_id = document_id;
		this.document_rank = document_rank;
		this.score = score;
		
	}


	public String getDocument_id() {
		return document_id;
	}


	public void setDocument_id(String document_id) {
		this.document_id = document_id;
	}


	public int getDocument_rank() {
		return document_rank;
	}


	public void setDocument_rank(int document_rank) {
		this.document_rank = document_rank;
	}


	public double getScore() {
		return score;
	}


	public void setScore(double score) {
		this.score = score;
	}

}
