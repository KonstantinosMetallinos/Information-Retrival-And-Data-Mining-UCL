package eval;

public class Qrel {
	String document_id ;
	int topic_no ;
	int subtopic_no ;
	int judgment ;
	
	/*
	 * Initialise the Qrel object with query, document and 
	 * its relevance grade.
	 */
	public Qrel(String topic, String doc_id, String rel) {

		this.topic_no = Integer.parseInt(topic);
		this.document_id = doc_id;
		this.judgment = Integer.parseInt(rel);
		
	}

	/*
	 * Initialise the Qrel object with query, subtopic, document and 
	 * its relevance grade with respect to query subtopic.
	 */
	public Qrel(String topic, String subtopic,  String doc_id, String rel) {

		this.topic_no = Integer.parseInt(topic);
		this.subtopic_no = Integer.parseInt(subtopic); 
		this.document_id = doc_id;
		this.judgment = Integer.parseInt(rel);
				
	}

	public String getDocument_id() {
		return document_id;
	}

	public void setDocument_id(String document_id) {
		this.document_id = document_id;
	}

	public int getTopic_no() {
		return topic_no;
	}

	public void setTopic_no(int topic_no) {
		this.topic_no = topic_no;
	}

	public int getSubtopic_no() {
		return subtopic_no;
	}

	public void setSubtopic_no(int subtopic_no) {
		this.subtopic_no = subtopic_no;
	}

	public int getJudgment() {
		return judgment;
	}

	public void setJudgment(int judgment) {
		this.judgment = judgment;
	}
	
	

}
